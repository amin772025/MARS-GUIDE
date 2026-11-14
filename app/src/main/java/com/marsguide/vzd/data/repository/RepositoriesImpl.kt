package com.marsguide.vzd.data.repository

import com.marsguide.vzd.data.local.MarsGuideDao
import com.marsguide.vzd.data.local.entity.SavedLocationEntity
import com.marsguide.vzd.data.local.entity.SavedRouteEntity
import com.marsguide.vzd.data.remote.AuthenticMarsDatasets
import com.marsguide.vzd.data.remote.NasaApiService
import com.marsguide.vzd.domain.model.*
import com.marsguide.vzd.domain.repository.*
import com.marsguide.vzd.util.MarsMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.util.UUID

class MarsLocationRepositoryImpl : MarsLocationRepository {
    override fun getAllLocations(): Flow<List<MarsLocation>> {
        return flowOf(AuthenticMarsDatasets.LOCATIONS)
    }

    override fun getLocationById(id: String): MarsLocation? {
        return AuthenticMarsDatasets.LOCATIONS.find { it.id == id }
    }
}

class MarsImageRepositoryImpl(
    private val apiService: NasaApiService = NasaApiService.create()
) : MarsImageRepository {

    override suspend fun getImagesForLocation(query: String): Result<List<MarsImage>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchImages(query = "Mars $query")
            val items = response.collection?.items.orEmpty()
            val images = items.mapNotNull { item ->
                val data = item.data?.firstOrNull() ?: return@mapNotNull null
                val link = item.links?.firstOrNull { it.render == "image" || it.rel == "preview" }?.href
                    ?: item.links?.firstOrNull()?.href
                    ?: return@mapNotNull null

                val secureUrl = if (link.startsWith("http://")) {
                    link.replace("http://", "https://")
                } else link

                MarsImage(
                    id = data.nasaId ?: UUID.randomUUID().toString(),
                    title = data.title ?: "NASA Mars Observation: $query",
                    mission = "NASA Mars Exploration Archive",
                    instrument = data.center ?: "NASA",
                    earthDate = data.dateCreated?.take(10),
                    imageUrl = secureUrl,
                    description = data.description ?: "Observation query for $query from NASA Image and Video Library.",
                    coordinate = null,
                    source = NASADataSource(
                        id = "NASA_IMAGE_VIDEO_LIBRARY",
                        name = "NASA Image and Video Library Public API",
                        organization = "NASA Headquarters / Johnson Space Center",
                        mission = "NASA Public Data API",
                        datasetId = "NASA-IVL-REST-API",
                        sourceUrl = "https://images.nasa.gov/",
                        retrievalInfo = "Live Query: ${data.nasaId ?: query}",
                        attributionDisclaimer = "Image content and metadata courtesy of NASA."
                    )
                )
            }

            if (images.isNotEmpty()) {
                Result.success(images.take(12))
            } else {
                // Fallback to curated offline image records
                Result.success(getCuratedOfflineImages(query))
            }
        } catch (e: Exception) {
            // Graceful offline fallback
            val offlineFallback = getCuratedOfflineImages(query)
            if (offlineFallback.isNotEmpty()) {
                Result.success(offlineFallback)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun getCuratedOfflineImages(query: String): List<MarsImage> {
        return listOf(
            MarsImage(
                id = "curated_mgs_mola_global",
                title = "MGS MOLA Mars Global Topography (Curated Offline)",
                mission = "Mars Global Surveyor",
                instrument = "MOLA (Mars Orbiter Laser Altimeter)",
                earthDate = "2001-05-24",
                imageUrl = "https://photojournal.jpl.nasa.gov/jpeg/PIA02820.jpg",
                description = "Official color-coded false-elevation hemispheric shaded relief map from the MGS MOLA experiment team.",
                source = AuthenticMarsDatasets.PDS_MOLA_SOURCE
            ),
            MarsImage(
                id = "curated_jezero_m2020",
                title = "Jezero Crater Delta deposits (Curated Offline)",
                mission = "Mars 2020 (Perseverance)",
                instrument = "Mastcam-Z / HiRISE",
                earthDate = "2021-03-12",
                imageUrl = "https://photojournal.jpl.nasa.gov/jpeg/PIA24479.jpg",
                description = "View of ancient delta remnants and western fan deposit in Jezero Crater.",
                source = AuthenticMarsDatasets.JPL_MARS2020_SOURCE
            ),
            MarsImage(
                id = "curated_gale_msl",
                title = "Gale Crater and Mount Sharp Stratigraphy (Curated Offline)",
                mission = "Mars Science Laboratory",
                instrument = "Mastcam",
                earthDate = "2015-09-09",
                imageUrl = "https://photojournal.jpl.nasa.gov/jpeg/PIA19839.jpg",
                description = "Curiosity panorama showing sandstone layering at the base of Aeolis Mons (Mount Sharp).",
                source = AuthenticMarsDatasets.JPL_MSL_SOURCE
            )
        )
    }
}

class MarsMissionRepositoryImpl : MarsMissionRepository {
    override fun getAllMissions(): List<MarsMission> = AuthenticMarsDatasets.MISSIONS
    override fun getAllRovers(): List<MarsRover> = AuthenticMarsDatasets.ROVERS
}

class MarsRouteRepositoryImpl(
    private val dao: MarsGuideDao
) : MarsRouteRepository {

    override fun calculateRoute(start: MarsLocation, dest: MarsLocation, numWaypoints: Int?): MarsRoute {
        val distanceKm = MarsMath.calculateGreatCircleDistanceKm(start.coordinate, dest.coordinate)
        val bearing = MarsMath.calculateInitialBearingDegrees(start.coordinate, dest.coordinate)
        val count = numWaypoints ?: 5
        val waypoints = MarsMath.generateGreatCircleWaypoints(start.coordinate, dest.coordinate, count)

        val elevChange = if (start.elevationMeters != null && dest.elevationMeters != null) {
            dest.elevationMeters - start.elevationMeters
        } else null

        return MarsRoute(
            id = "route_${start.id}_to_${dest.id}",
            name = "${start.name} ➔ ${dest.name}",
            startLocationName = start.name,
            startCoordinate = start.coordinate,
            destinationLocationName = dest.name,
            destinationCoordinate = dest.coordinate,
            waypoints = waypoints,
            distanceKm = distanceKm,
            initialBearingDegrees = bearing,
            elevationChangeMeters = elevChange
        )
    }

    override suspend fun saveRoute(route: MarsRoute) {
        val entity = SavedRouteEntity(
            id = route.id,
            name = route.name,
            startLocationName = route.startLocationName,
            startLatitude = route.startCoordinate.latitude,
            startLongitude = route.startCoordinate.normalizedLongitude,
            startElevation = route.startCoordinate.elevation,
            destinationLocationName = route.destinationLocationName,
            destinationLatitude = route.destinationCoordinate.latitude,
            destinationLongitude = route.destinationCoordinate.normalizedLongitude,
            destinationElevation = route.destinationCoordinate.elevation,
            distanceKm = route.distanceKm,
            initialBearingDegrees = route.initialBearingDegrees,
            elevationChangeMeters = route.elevationChangeMeters,
            waypointsJson = route.waypoints.joinToString(";") { "${it.latitude},${it.normalizedLongitude},${it.elevation ?: 0.0}" }
        )
        dao.insertSavedRoute(entity)
    }

    override suspend fun deleteSavedRoute(id: String) {
        dao.deleteSavedRoute(id)
    }
}

class SavedRepositoryImpl(
    private val dao: MarsGuideDao
) : SavedRepository {

    override fun getSavedLocations(): Flow<List<SavedLocationEntity>> {
        return dao.getSavedLocations()
    }

    override fun getSavedRoutes(): Flow<List<SavedRouteEntity>> {
        return dao.getSavedRoutes()
    }

    override fun isLocationSaved(id: String): Flow<Boolean> {
        return dao.isLocationSaved(id)
    }

    override suspend fun saveLocation(location: MarsLocation) {
        val entity = SavedLocationEntity(
            id = location.id,
            name = location.name,
            type = location.type,
            latitude = location.coordinate.latitude,
            longitude = location.coordinate.normalizedLongitude,
            elevationMeters = location.elevationMeters,
            diameterKm = location.diameterKm,
            description = location.description
        )
        dao.insertSavedLocation(entity)
    }

    override suspend fun removeSavedLocation(id: String) {
        dao.deleteSavedLocation(id)
    }
}
