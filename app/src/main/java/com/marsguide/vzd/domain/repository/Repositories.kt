package com.marsguide.vzd.domain.repository

import com.marsguide.vzd.data.local.entity.SavedLocationEntity
import com.marsguide.vzd.data.local.entity.SavedRouteEntity
import com.marsguide.vzd.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MarsLocationRepository {
    fun getAllLocations(): Flow<List<MarsLocation>>
    fun getLocationById(id: String): MarsLocation?
}

interface MarsImageRepository {
    suspend fun getImagesForLocation(query: String): Result<List<MarsImage>>
}

interface MarsMissionRepository {
    fun getAllMissions(): List<MarsMission>
    fun getAllRovers(): List<MarsRover>
}

interface MarsRouteRepository {
    fun calculateRoute(start: MarsLocation, dest: MarsLocation, numWaypoints: Int? = null): MarsRoute
    suspend fun saveRoute(route: MarsRoute)
    suspend fun deleteSavedRoute(id: String)
}

interface SavedRepository {
    fun getSavedLocations(): Flow<List<SavedLocationEntity>>
    fun getSavedRoutes(): Flow<List<SavedRouteEntity>>
    fun isLocationSaved(id: String): Flow<Boolean>
    suspend fun saveLocation(location: MarsLocation)
    suspend fun removeSavedLocation(id: String)
}
