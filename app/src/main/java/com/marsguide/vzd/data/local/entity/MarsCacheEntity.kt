package com.marsguide.vzd.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mars_cache")
data class MarsCacheEntity(
    @PrimaryKey val cacheKey: String,
    val jsonData: String,
    val timestamp: Long = System.currentTimeMillis()
)
