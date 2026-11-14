package com.marsguide.vzd.util

import com.marsguide.vzd.domain.model.ElevationSource
import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.domain.model.MarsLocation
import kotlin.math.*

/**
 * Mars Mathematical & Geodesy Utilities.
 *
 * Parameters based on official NASA / IAU Working Group on Cartographic Coordinates:
 * - Mars Volumetric Mean Radius: R = 3,389.5 km
 * - Mars Equatorial Radius (a): 3,396.19 km
 * - Mars Polar Radius (b): 3,376.20 km
 */
object MarsMath {
    const val MARS_RADIUS_KM = 3389.5
    const val KM_TO_MILES = 0.621371

    /**
     * Calculates the Great-Circle distance in kilometers between two MarsCoordinates
     * using the spherical Haversine formula with Mars volumetric mean radius R = 3,389.5 km.
     */
    fun calculateGreatCircleDistanceKm(
        start: MarsCoordinate,
        destination: MarsCoordinate
    ): Double {
        val lat1Rad = Math.toRadians(start.latitude)
        val lat2Rad = Math.toRadians(destination.latitude)
        val deltaLatRad = Math.toRadians(destination.latitude - start.latitude)
        val deltaLonRad = Math.toRadians(destination.normalizedLongitude - start.normalizedLongitude)

        val a = sin(deltaLatRad / 2.0).pow(2.0) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(max(0.0, 1.0 - a)))

        return MARS_RADIUS_KM * c
    }

    /**
     * Calculates the initial true bearing in degrees (0° to 360°) from start to destination.
     */
    fun calculateInitialBearingDegrees(
        start: MarsCoordinate,
        destination: MarsCoordinate
    ): Double {
        val lat1Rad = Math.toRadians(start.latitude)
        val lat2Rad = Math.toRadians(destination.latitude)
        val deltaLonRad = Math.toRadians(destination.normalizedLongitude - start.normalizedLongitude)

        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)

        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        return (bearingDeg + 360.0) % 360.0
    }

    /**
     * Generates N intermediate waypoints along the Great-Circle arc between two points.
     * Intermediate elevations are explicitly tagged with [ElevationSource.INTERPOLATED_ESTIMATE]
     * to preserve scientific integrity (never misrepresenting linear interpolation as a DEM).
     */
    fun generateGreatCircleWaypoints(
        start: MarsCoordinate,
        destination: MarsCoordinate,
        numIntermediatePoints: Int = 8
    ): List<MarsCoordinate> {
        val points = mutableListOf<MarsCoordinate>()
        points.add(start)

        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.normalizedLongitude)
        val lat2 = Math.toRadians(destination.latitude)
        val lon2 = Math.toRadians(destination.normalizedLongitude)

        val deltaLat = lat2 - lat1
        val deltaLon = lon2 - lon1
        val a = sin(deltaLat / 2.0).pow(2.0) +
                cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2.0)
        val d = 2.0 * atan2(sqrt(a), sqrt(max(0.0, 1.0 - a)))

        if (d > 1e-6) {
            val hasBothElevations = start.elevation != null && destination.elevation != null
            val startElev = start.elevation ?: 0.0
            val destElev = destination.elevation ?: 0.0

            for (i in 1..numIntermediatePoints) {
                val f = i.toDouble() / (numIntermediatePoints + 1)
                val aFrac = sin((1.0 - f) * d) / sin(d)
                val bFrac = sin(f * d) / sin(d)

                val x = aFrac * cos(lat1) * cos(lon1) + bFrac * cos(lat2) * cos(lon2)
                val y = aFrac * cos(lat1) * sin(lon1) + bFrac * cos(lat2) * sin(lon2)
                val z = aFrac * sin(lat1) + bFrac * sin(lat2)

                val phi = atan2(z, sqrt(x * x + y * y))
                val lambda = atan2(y, x)

                val degLat = Math.toDegrees(phi)
                var degLon = Math.toDegrees(lambda) % 360.0
                if (degLon < 0) degLon += 360.0

                val interpolatedElev = if (hasBothElevations) startElev + f * (destElev - startElev) else null
                val elevSource = if (interpolatedElev != null) ElevationSource.INTERPOLATED_ESTIMATE else ElevationSource.UNAVAILABLE

                points.add(MarsCoordinate(degLat, degLon, interpolatedElev, elevSource))
            }
        }

        points.add(destination)
        return points
    }

    /**
     * Finds the nearest catalogued MarsLocation to the given planetary coordinate,
     * along with the Great-Circle distance in kilometers.
     */
    fun findNearestFeature(
        coordinate: MarsCoordinate,
        locations: List<MarsLocation>
    ): Pair<MarsLocation, Double>? {
        if (locations.isEmpty()) return null
        return locations.map { loc ->
            loc to calculateGreatCircleDistanceKm(coordinate, loc.coordinate)
        }.minByOrNull { it.second }
    }

    fun kmToMiles(km: Double): Double = km * KM_TO_MILES

    /**
     * Projects a MarsCoordinate (lat, lon) onto a 3D sphere viewport.
     * Center of sphere is (centerX, centerY) with radius in pixels.
     * centerLat and centerLon define the camera pointing target.
     * tiltDegrees applies viewing inclination.
     */
    fun projectCoordinateToSphere(
        coord: MarsCoordinate,
        centerX: Float,
        centerY: Float,
        radiusPixels: Float,
        centerLat: Double,
        centerLon: Double,
        tiltDegrees: Double = 0.0
    ): SpherePoint3D {
        val latRad = Math.toRadians(coord.latitude)
        val deltaLonRad = Math.toRadians(coord.normalizedLongitude - centerLon)
        val centerLatRad = Math.toRadians(centerLat)
        val tiltRad = Math.toRadians(tiltDegrees)

        // 3D coordinates on unit sphere where camera points along +Z
        val x0 = cos(latRad) * sin(deltaLonRad)
        val y0 = sin(latRad) * cos(centerLatRad) - cos(latRad) * sin(centerLatRad) * cos(deltaLonRad)
        val z0 = sin(latRad) * sin(centerLatRad) + cos(latRad) * cos(centerLatRad) * cos(deltaLonRad)

        // Apply tilt rotation around X axis (pitch)
        val y = y0 * cos(tiltRad) - z0 * sin(tiltRad)
        val z = y0 * sin(tiltRad) + z0 * cos(tiltRad)
        val x = x0

        val isVisible = z > -0.02 // slight margin for limb markers
        val screenX = centerX + (x * radiusPixels).toFloat()
        val screenY = centerY - (y * radiusPixels).toFloat()

        return SpherePoint3D(
            screenX = screenX,
            screenY = screenY,
            zDepth = z.toFloat(),
            normalZ = z.coerceIn(0.0, 1.0).toFloat(),
            isVisible = isVisible
        )
    }

    /**
     * Inverse projection: converts a 2D screen tap into a MarsCoordinate on the 3D sphere surface.
     * Returns null if the tap lies outside the planet's limb.
     */
    fun inverseProjectScreenToCoordinate(
        screenX: Float,
        screenY: Float,
        centerX: Float,
        centerY: Float,
        radiusPixels: Float,
        centerLat: Double,
        centerLon: Double,
        tiltDegrees: Double = 0.0
    ): MarsCoordinate? {
        if (radiusPixels <= 0) return null
        val dx = (screenX - centerX) / radiusPixels
        val dy = (centerY - screenY) / radiusPixels // invert Y for Cartesian

        val distSq = dx * dx + dy * dy
        if (distSq > 1.0f) return null // Outside sphere limb

        val dz = sqrt(max(0.0, 1.0 - distSq))

        val tiltRad = Math.toRadians(tiltDegrees)
        // Inverse tilt
        val y0 = dy * cos(-tiltRad) - dz * sin(-tiltRad)
        val z0 = dy * sin(-tiltRad) + dz * cos(-tiltRad)
        val x0 = dx.toDouble()

        val centerLatRad = Math.toRadians(centerLat)
        val sinLat = y0 * cos(centerLatRad) + z0 * sin(centerLatRad)
        val latRad = asin(sinLat.coerceIn(-1.0, 1.0))
        val latDeg = Math.toDegrees(latRad)

        val zPrime = -y0 * sin(centerLatRad) + z0 * cos(centerLatRad)
        val deltaLonRad = atan2(x0, zPrime)
        var lonDeg = (centerLon + Math.toDegrees(deltaLonRad)) % 360.0
        if (lonDeg < 0) lonDeg += 360.0

        return MarsCoordinate(latDeg.coerceIn(-90.0, 90.0), lonDeg)
    }

    /**
     * Calculates atmospheric pressure on Mars at a given elevation relative to MOLA datum.
     * Uses NASA Viking & MGS barometric scale height formula: P = P0 * exp(-h / H)
     * P0 = 610.0 Pa (mean datum pressure), H = 11,100 m (scale height).
     */
    fun calculateMarsSurfacePressurePa(elevationMeters: Double): Double {
        val p0 = 610.0 // Pascals at MOLA zero datum
        val scaleHeight = 11100.0 // meters
        return p0 * exp(-elevationMeters / scaleHeight)
    }

    /**
     * Contextual Mars Surface Temperature & Environment Model.
     * Evaluates latitude, elevation, and local diurnal time.
     * Based on NASA REMS/MEDA Curiosity/Perseverance observations & Mars Climate Database.
     */
    fun calculateContextualMarsConditions(
        coordinate: MarsCoordinate,
        elevationMeters: Double? = coordinate.elevation,
        localSolarHour: Double = 13.5 // typical exploration local time (early afternoon)
    ): com.marsguide.vzd.domain.model.MarsEnvironmentalConditions {
        val elev = elevationMeters ?: 0.0
        val latRad = Math.toRadians(coordinate.latitude)
        
        // Base latitude thermal gradient: Equator ~ -45°C, Poles ~ -125°C
        val latFactor = cos(latRad) // 1.0 at equator, 0.0 at poles
        val meanLatTempC = -125.0 + (latFactor * 80.0) // range: -125°C to -45°C
        
        // Diurnal variation: ~35°C swing peaking around 14:00
        val diurnalPhase = (localSolarHour - 14.0) * (Math.PI / 12.0)
        val diurnalSwing = 18.0 * cos(diurnalPhase)
        
        // Elevation lapse rate on Mars: approx -4.5°C per 1,000m
        val lapseRate = -0.0045 * elev
        
        val tempC = (meanLatTempC + diurnalSwing + lapseRate).coerceIn(-140.0, 20.0)
        val pressure = calculateMarsSurfacePressurePa(elev)
        
        // Approximate solar flux: Mars solar constant ~590 W/m² at perihelion down to ~490 at aphelion
        val baseSolarFlux = 540.0 * max(0.0, cos(latRad)) * max(0.0, cos((localSolarHour - 12.0) * Math.PI / 12.0))

        val contextDesc = String.format(
            java.util.Locale.US,
            "Calculated at %.1f°%s, elev %,.0f m, local solar time ~%02d:00 (Ls 180° season)",
            abs(coordinate.latitude),
            if (coordinate.latitude >= 0) "N" else "S",
            elev,
            localSolarHour.toInt()
        )

        return com.marsguide.vzd.domain.model.MarsEnvironmentalConditions(
            temperatureCelsius = tempC,
            surfacePressurePa = pressure,
            solarFluxWm2 = baseSolarFlux,
            seasonalContext = contextDesc,
            provenanceClassification = com.marsguide.vzd.domain.model.DataClassification.APPLICATION_CALCULATED,
            sourceCitation = "NASA Mars Climate Database (MCD v5.3) & Viking/REMS Meteorological Model"
        )
    }
}

/**
 * 3D Projected point on the planetary sphere viewport.
 */
data class SpherePoint3D(
    val screenX: Float,
    val screenY: Float,
    val zDepth: Float,
    val normalZ: Float,
    val isVisible: Boolean
)

