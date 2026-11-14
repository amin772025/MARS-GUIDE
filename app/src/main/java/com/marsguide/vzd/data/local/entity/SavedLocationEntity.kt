package com.marsguide.vzd.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.domain.model.MarsLocation

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double?,
    val diameterKm: Double?,
    val description: String,
    val savedAt: Long = System.currentTimeMillis()
)
