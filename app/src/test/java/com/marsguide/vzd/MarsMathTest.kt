package com.marsguide.vzd

import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.util.MarsMath
import org.junit.Assert.*
import org.junit.Test

class MarsMathTest {

    @Test
    fun marsCoordinate_validRangesAndFormatting() {
        val jezero = MarsCoordinate(latitude = 18.4447, longitude = 77.4508, elevation = -2540.0)
        assertTrue(jezero.isValid())
        assertEquals("18.4447° N", jezero.formatLatitude())
        assertEquals("77.4508° E", jezero.formatLongitude())

        val gale = MarsCoordinate(latitude = -5.37, longitude = 137.81, elevation = -4490.0)
        assertTrue(gale.isValid())
        assertEquals("5.3700° S", gale.formatLatitude())
        assertEquals("137.8100° E", gale.formatLongitude())
    }

    @Test
    fun greatCircleDistance_samePointIsZero() {
        val point = MarsCoordinate(latitude = 0.0, longitude = 100.0)
        val dist = MarsMath.calculateGreatCircleDistanceKm(point, point)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun greatCircleDistance_equatorialQuarterCircumference() {
        // From (0°, 0°E) to (0°, 90°E) along equator: angle = pi / 2
        // Distance should be (pi / 2) * R_Mars = 1.570796 * 3389.5 ≈ 5324.23 km
        val p1 = MarsCoordinate(0.0, 0.0)
        val p2 = MarsCoordinate(0.0, 90.0)
        val dist = MarsMath.calculateGreatCircleDistanceKm(p1, p2)
        val expected = (Math.PI / 2.0) * MarsMath.MARS_RADIUS_KM
        assertEquals(expected, dist, 1.0)
    }

    @Test
    fun greatCircleDistance_betweenJezeroAndGale() {
        // Jezero: (18.4447°N, 77.4508°E)
        // Gale: (5.37°S, 137.81°E)
        val jezero = MarsCoordinate(18.4447, 77.4508)
        val gale = MarsCoordinate(-5.37, 137.81)
        val dist = MarsMath.calculateGreatCircleDistanceKm(jezero, gale)
        // Approximate distance on Mars is around 3,700 - 4,000 km
        assertTrue("Distance $dist should be between 3500 and 4200 km", dist in 3500.0..4200.0)
    }

    @Test
    fun initialBearing_eastwardAlongEquator() {
        val p1 = MarsCoordinate(0.0, 10.0)
        val p2 = MarsCoordinate(0.0, 20.0)
        val bearing = MarsMath.calculateInitialBearingDegrees(p1, p2)
        // Heading East along equator is 90 degrees
        assertEquals(90.0, bearing, 0.5)
    }

    @Test
    fun waypoints_countAndEndpoints() {
        val p1 = MarsCoordinate(10.0, 20.0, 100.0)
        val p2 = MarsCoordinate(20.0, 40.0, 500.0)
        val waypoints = MarsMath.generateGreatCircleWaypoints(p1, p2, numIntermediatePoints = 5)

        assertEquals(7, waypoints.size) // start + 5 intermediate + destination
        assertEquals(p1.latitude, waypoints.first().latitude, 0.001)
        assertEquals(p2.latitude, waypoints.last().latitude, 0.001)
    }

    @Test
    fun kmToMilesConversion() {
        val km = 100.0
        val miles = MarsMath.kmToMiles(km)
        assertEquals(62.1371, miles, 0.01)
    }

    @Test
    fun waypoints_elevationProvenance() {
        val p1 = MarsCoordinate(10.0, 20.0, 100.0, com.marsguide.vzd.domain.model.ElevationSource.MOLA_PDS_DATUM)
        val p2 = MarsCoordinate(20.0, 40.0, 500.0, com.marsguide.vzd.domain.model.ElevationSource.MOLA_PDS_DATUM)
        val waypoints = MarsMath.generateGreatCircleWaypoints(p1, p2, numIntermediatePoints = 3)

        assertEquals(5, waypoints.size)
        assertEquals(com.marsguide.vzd.domain.model.ElevationSource.MOLA_PDS_DATUM, waypoints.first().elevationSource)
        assertEquals(com.marsguide.vzd.domain.model.ElevationSource.MOLA_PDS_DATUM, waypoints.last().elevationSource)
        // Intermediate points must be tagged as INTERPOLATED_ESTIMATE for scientific integrity
        assertEquals(com.marsguide.vzd.domain.model.ElevationSource.INTERPOLATED_ESTIMATE, waypoints[1].elevationSource)
        assertEquals(com.marsguide.vzd.domain.model.ElevationSource.INTERPOLATED_ESTIMATE, waypoints[2].elevationSource)
        assertEquals(com.marsguide.vzd.domain.model.ElevationSource.INTERPOLATED_ESTIMATE, waypoints[3].elevationSource)
    }

    @Test
    fun findNearestFeature_returnsClosestLandmark() {
        val jezero = com.marsguide.vzd.data.remote.AuthenticMarsDatasets.LOCATIONS.first { it.id == "loc_jezero" }
        val testCoord = MarsCoordinate(latitude = 18.5, longitude = 77.5) // ~10 km from Jezero center
        val nearest = MarsMath.findNearestFeature(testCoord, com.marsguide.vzd.data.remote.AuthenticMarsDatasets.LOCATIONS)

        assertNotNull(nearest)
        assertEquals("loc_jezero", nearest!!.first.id)
        assertTrue("Distance should be less than 50 km", nearest.second < 50.0)
    }

    @Test
    fun sphericalProjection_centerOfViewIsVisibleAndCentered() {
        val center = MarsCoordinate(0.0, 100.0)
        val cx = 500f
        val cy = 500f
        val radius = 300f

        val pt = MarsMath.projectCoordinateToSphere(
            coord = center,
            centerX = cx,
            centerY = cy,
            radiusPixels = radius,
            centerLat = 0.0,
            centerLon = 100.0,
            tiltDegrees = 0.0
        )

        assertTrue(pt.isVisible)
        assertEquals(cx, pt.screenX, 1.0f)
        assertEquals(cy, pt.screenY, 1.0f)
    }

    @Test
    fun sphericalProjection_antipodeIsOccluded() {
        // Center is at 0°N, 0°E; Antipode is at 0°N, 180°E (far side of Mars)
        val antipode = MarsCoordinate(0.0, 180.0)
        val pt = MarsMath.projectCoordinateToSphere(
            coord = antipode,
            centerX = 400f,
            centerY = 400f,
            radiusPixels = 250f,
            centerLat = 0.0,
            centerLon = 0.0,
            tiltDegrees = 0.0
        )

        assertFalse("Antipode on far side of globe must be occluded", pt.isVisible)
    }

    @Test
    fun inverseSphericalProjection_centerReturnsCenterCoordinate() {
        val cx = 400f
        val cy = 400f
        val radius = 250f
        val centerLat = 15.0
        val centerLon = 80.0

        val unprojected = MarsMath.inverseProjectScreenToCoordinate(
            screenX = cx,
            screenY = cy,
            centerX = cx,
            centerY = cy,
            radiusPixels = radius,
            centerLat = centerLat,
            centerLon = centerLon,
            tiltDegrees = 0.0
        )

        assertNotNull(unprojected)
        assertEquals(centerLat, unprojected!!.latitude, 0.5)
        assertEquals(centerLon, unprojected.longitude, 0.5)
    }

    @Test
    fun contextualMarsConditions_physicalSanityCheck() {
        val olympusMons = MarsCoordinate(latitude = 18.65, longitude = 226.2, elevation = 21287.0)
        val hellasBasin = MarsCoordinate(latitude = -42.4, longitude = 70.5, elevation = -7152.0)

        val olympusConditions = MarsMath.calculateContextualMarsConditions(olympusMons)
        val hellasConditions = MarsMath.calculateContextualMarsConditions(hellasBasin)

        // Mars surface pressure scale: Hellas Basin floor has highest pressure, Olympus Mons summit has lowest
        assertTrue(
            "Olympus Mons pressure (${olympusConditions.surfacePressurePa} Pa) must be lower than Hellas Basin (${hellasConditions.surfacePressurePa} Pa)",
            olympusConditions.surfacePressurePa < hellasConditions.surfacePressurePa
        )

        // Temperatures must be within realistic Martian bounds (-130°C to +20°C)
        assertTrue(olympusConditions.temperatureCelsius in -130.0..25.0)
        assertTrue(hellasConditions.temperatureCelsius in -130.0..25.0)
    }
}
