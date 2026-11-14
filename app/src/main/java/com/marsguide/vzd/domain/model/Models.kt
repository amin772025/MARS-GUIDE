package com.marsguide.vzd.domain.model

/**
 * Scientific Data Classification System.
 * Distinguishes verified NASA/USGS primary sources from calculations and estimates.
 */
enum class DataClassification(
    val badgeLabel: String,
    val fullTitle: String,
    val description: String
) {
    REAL_LIVE_API(
        badgeLabel = "REAL LIVE DATA",
        fullTitle = "Real Live NASA API Query",
        description = "Live network response directly queried from NASA public API endpoints (e.g. NASA Image and Video Library)."
    ),
    STATIC_SOURCE_DERIVED(
        badgeLabel = "STATIC SOURCE-DERIVED",
        fullTitle = "NASA / USGS Static Source-Derived",
        description = "Authentic scientific facts extracted from official NASA PDS, USGS Astrogeology Gazetteer, and JPL Mission Archives."
    ),
    APPLICATION_CALCULATED(
        badgeLabel = "APPLICATION-CALCULATED",
        fullTitle = "MARS//GUIDE On-Device Geodesy",
        description = "Mathematically calculated on-device by MARS//GUIDE (e.g. spherical Great-Circle distance, forward bearing, waypoint slerp)."
    ),
    ESTIMATED_APPROXIMATION(
        badgeLabel = "ESTIMATED APPROXIMATION",
        fullTitle = "Estimated / Approximation Model",
        description = "Linear interpolation or generalized physical approximation (e.g. crustal dichotomy shading, interpolated waypoint height). Not a raw DEM."
    )
}

/**
 * Elevation Data Provenance & Verification Status.
 * Clearly separates official MOLA datum from linear interpolations.
 */
enum class ElevationSource(
    val label: String,
    val shortBadge: String,
    val isVerifiedDatum: Boolean
) {
    MOLA_PDS_DATUM(
        label = "NASA PDS MOLA MEGDR Datum (±1m relative to 3,396.19 km areoid)",
        shortBadge = "MOLA MEGDR",
        isVerifiedDatum = true
    ),
    INTERPOLATED_ESTIMATE(
        label = "Estimated/interpolated elevation — not a DEM terrain profile",
        shortBadge = "EST. INTERPOLATED",
        isVerifiedDatum = false
    ),
    UNAVAILABLE(
        label = "Elevation Unavailable — No continuous DEM raster lookup at this coordinate",
        shortBadge = "UNAVAILABLE",
        isVerifiedDatum = false
    )
}

/**
 * Toggleable Map Layer Identifier.
 */
enum class MapLayerId {
    BASEMAP,
    TOPOGRAPHY,
    GRATICULE,
    GEOLOGICAL_FEATURES,
    LANDING_SITES,
    ROVERS,
    SCIENCE_TARGETS,
    ACTIVE_ROUTE
}

/**
 * Configuration & Provenance for each toggleable Map Layer.
 */
data class MapLayerConfig(
    val id: MapLayerId,
    val title: String,
    val description: String,
    val classification: DataClassification,
    val sourceCitation: String,
    val isEnabled: Boolean = true
)

/**
 * Mars Coordinate Model.
 *
 * Coordinate Reference Assumptions:
 * - Latitude: Planetocentric latitude in decimal degrees [-90.0° to +90.0°].
 *   Positive values are Northern hemisphere, negative values are Southern hemisphere.
 * - Longitude: East Longitude convention in decimal degrees [0.0° to 360.0°].
 *   Standardized in accordance with IAU/IAG 2000 Mars Aerocentric cartographic coordinate system.
 * - Elevation: Elevation in meters relative to the Mars MOLA (Mars Orbiter Laser Altimeter)
 *   Areoid reference datum (the Martian equipotential zero-elevation gravitational surface
 *   with mean equatorial radius R_eq = 3396.19 km).
 * - Mean Volumetric Mars Radius for spherical geodesy: R_Mars = 3,389.5 km.
 */
data class MarsCoordinate(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val elevationSource: ElevationSource = if (elevation != null) ElevationSource.MOLA_PDS_DATUM else ElevationSource.UNAVAILABLE
) {
    init {
        require(latitude in -90.0..90.0) {
            "Latitude $latitude is out of valid Mars planetocentric range [-90.0, 90.0]"
        }
        // Normalize longitude to 0..360 if within -180..360
        require(longitude in -360.0..720.0) {
            "Longitude $longitude is out of valid range"
        }
    }

    /** Returns normalized East longitude in [0.0, 360.0) degrees */
    val normalizedLongitude: Double
        get() {
            var lon = longitude % 360.0
            if (lon < 0) lon += 360.0
            return lon
        }

    fun isValid(): Boolean {
        return latitude in -90.0..90.0 && longitude in 0.0..360.0
    }

    fun formatLatitude(): String {
        val dir = if (latitude >= 0) "N" else "S"
        return String.format(java.util.Locale.US, "%.4f° %s", kotlin.math.abs(latitude), dir)
    }

    fun formatLongitude(): String {
        return String.format(java.util.Locale.US, "%.4f° E", normalizedLongitude)
    }

    fun formatShort(): String {
        return "${formatLatitude()}, ${formatLongitude()}"
    }

    fun formatWithElevation(): String {
        val elevStr = elevation?.let { String.format(java.util.Locale.US, " | Elev: %,.0f m (%s)", it, elevationSource.shortBadge) } ?: ""
        return "${formatShort()}$elevStr"
    }
}

/**
 * Official NASA Planetary Data Source documentation record.
 * Required for every dataset shown to preserve scientific integrity.
 */
data class NASADataSource(
    val id: String,
    val name: String,
    val organization: String = "NASA Planetary Data System (PDS)",
    val mission: String,
    val instrument: String? = null,
    val datasetId: String,
    val sourceUrl: String,
    val retrievalInfo: String = "Public NASA Planetary Data System Archive",
    val coordinateSystem: String = "Planetocentric East Longitude (IAU/IAG 2000 Mars Datum)",
    val units: String = "Latitude/Longitude: decimal degrees; Elevation: meters relative to MOLA Areoid",
    val attributionDisclaimer: String = "Data provided by NASA PDS / USGS Astrogeology Science Center. Independent NASA Space Apps Challenge project; not an official NASA product."
)

/**
 * Martian Geographic Location backed by real USGS/IAU & NASA PDS Gazetteer records.
 */
data class MarsLocation(
    val id: String,
    val name: String,
    val type: String,
    val coordinate: MarsCoordinate,
    val diameterKm: Double? = null,
    val elevationMeters: Double? = null,
    val quadrangle: String? = null,
    val description: String,
    val geologicalContext: String,
    val terrainClassification: String,
    val pdsDatasetId: String,
    val source: NASADataSource,
    val imageUrl: String? = null
)

/**
 * Planetary Science Target backed by mission observation databases.
 */
data class ScienceTarget(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val targetType: String,
    val mission: String,
    val instrument: String,
    val sourceUrl: String,
    val imageUrl: String? = null,
    val elevationMeters: Double? = null
)

/**
 * NASA Mars Mission model.
 */
data class MarsMission(
    val id: String,
    val name: String,
    val type: String, // Rover, Orbiter, Lander
    val target: String = "Mars",
    val launchDate: String,
    val landingOrArrivalDate: String?,
    val status: String, // Active, Mission Completed, Extended Mission
    val relevantInstruments: List<String>,
    val description: String,
    val landingCoordinate: MarsCoordinate? = null,
    val landingSiteName: String? = null,
    val nasaSource: NASADataSource
)

/**
 * NASA Mars Rover model with authentic landing coordinates and telemetry.
 */
data class MarsRover(
    val id: String,
    val name: String,
    val mission: String,
    val landingLocation: String,
    val landingCoordinate: MarsCoordinate,
    val currentLocationName: String? = null,
    val landingDate: String,
    val operationalStatus: String,
    val totalDistanceTraveledKm: Double? = null,
    val instruments: List<String>,
    val source: NASADataSource
)

/**
 * Mars Trajectory / Route model using spherical geodesy on Mars.
 */
data class MarsRoute(
    val id: String,
    val name: String,
    val startLocationName: String,
    val startCoordinate: MarsCoordinate,
    val destinationLocationName: String,
    val destinationCoordinate: MarsCoordinate,
    val waypoints: List<MarsCoordinate>,
    val distanceKm: Double,
    val initialBearingDegrees: Double,
    val elevationChangeMeters: Double? = null,
    val minElevationMeters: Double? = null,
    val maxElevationMeters: Double? = null,
    val totalAscentMeters: Double? = null,
    val totalDescentMeters: Double? = null,
    val elevationStatus: String = "Endpoints from NASA PDS MOLA MEGDR; Intermediate waypoints: Estimated/interpolated elevation — not a DEM terrain profile.",
    val traversabilityStatus: String = "Geodesic mathematical planning approximation only. Local terrain slopes, escarpments, sand dunes, and rover trafficability are not evaluated.",
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

/**
 * NASA Mars Imagery Record from HiRISE, Mastcam, THEMIS, or CTX.
 */
data class MarsImage(
    val id: String,
    val title: String,
    val mission: String,
    val instrument: String,
    val sol: Int? = null,
    val earthDate: String? = null,
    val imageUrl: String,
    val description: String,
    val coordinate: MarsCoordinate? = null,
    val source: NASADataSource
)

/**
 * Authentic NASA Mission Rover Panorama Model.
 */
data class MarsPanorama(
    val id: String,
    val locationId: String,
    val title: String,
    val roverName: String,
    val mission: String,
    val sol: Int,
    val instrument: String,
    val panoramaUrl: String,
    val source: NASADataSource,
    val classification: DataClassification = DataClassification.STATIC_SOURCE_DERIVED,
    val description: String,
    val fieldOfViewHorizontalDeg: Double = 360.0,
    val fieldOfViewVerticalDeg: Double = 60.0
)

/**
 * Contextual Mars Environmental Conditions (Temperature, Pressure, Atmosphere).
 * Never fabricated; calculated using authentic NASA Mars Climate Database and Viking/REMS models.
 */
data class MarsEnvironmentalConditions(
    val temperatureCelsius: Double,
    val surfacePressurePa: Double,
    val solarFluxWm2: Double,
    val seasonalContext: String,
    val provenanceClassification: DataClassification,
    val sourceCitation: String
) {
    val temperatureFahrenheit: Double
        get() = (temperatureCelsius * 9.0 / 5.0) + 32.0

    fun formatTemperature(useFahrenheit: Boolean = false): String {
        return if (useFahrenheit) {
            String.format(java.util.Locale.US, "%.1f°F", temperatureFahrenheit)
        } else {
            String.format(java.util.Locale.US, "%.1f°C", temperatureCelsius)
        }
    }

    fun formatPressure(): String {
        return String.format(java.util.Locale.US, "%.0f Pa (%.2f mbar)", surfacePressurePa, surfacePressurePa / 100.0)
    }
}

/**
 * Educational Primer Entry.
 */
data class EducationalTopic(
    val id: String,
    val questionResId: Int,
    val answerResId: Int,
    val category: String,
    val officialPdsReference: String
)
