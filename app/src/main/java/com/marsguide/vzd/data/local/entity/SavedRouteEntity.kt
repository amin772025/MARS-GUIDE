package com.marsguide.vzd.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startLocationName: String,
    val startLatitude: Double,
    val startLongitude: Double,
    val startElevation: Double?,
    val destinationLocationName: String,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val destinationElevation: Double?,
    val distanceKm: Double,
    val initialBearingDegrees: Double,
    val elevationChangeMeters: Double?,
    val waypointsJson: String,
    val savedAt: Long = System.currentTimeMillis()
)
