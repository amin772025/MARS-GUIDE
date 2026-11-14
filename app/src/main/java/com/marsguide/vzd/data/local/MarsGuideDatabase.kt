package com.marsguide.vzd.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.marsguide.vzd.data.local.entity.SavedLocationEntity
import com.marsguide.vzd.data.local.entity.SavedRouteEntity
import com.marsguide.vzd.data.local.entity.MarsCacheEntity

@Database(
    entities = [SavedLocationEntity::class, SavedRouteEntity::class, MarsCacheEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MarsGuideDatabase : RoomDatabase() {
    abstract fun marsGuideDao(): MarsGuideDao

    companion object {
        @Volatile
        private var INSTANCE: MarsGuideDatabase? = null

        fun getInstance(context: Context): MarsGuideDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarsGuideDatabase::class.java,
                    "mars_guide_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
