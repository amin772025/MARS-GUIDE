package com.marsguide.vzd.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marsguide.vzd.data.local.entity.SavedLocationEntity
import com.marsguide.vzd.data.local.entity.SavedRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarsGuideDao {
    @Query("SELECT * FROM saved_locations ORDER BY savedAt DESC")
    fun getSavedLocations(): Flow<List<SavedLocationEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_locations WHERE id = :id)")
    fun isLocationSaved(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedLocation(location: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteSavedLocation(id: String)

    @Query("SELECT * FROM saved_routes ORDER BY savedAt DESC")
    fun getSavedRoutes(): Flow<List<SavedRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteSavedRoute(id: String)

    @Query("SELECT * FROM mars_cache WHERE cacheKey = :key")
    suspend fun getCache(key: String): com.marsguide.vzd.data.local.entity.MarsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: com.marsguide.vzd.data.local.entity.MarsCacheEntity)
}
