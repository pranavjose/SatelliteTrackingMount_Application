package com.pranavj.satellitetrackingmount.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Define the SatelliteDatabase class with RoomDatabase as the base class
@Database(entities = [Satellite::class], version = 1, exportSchema = false)
abstract class SatelliteDatabase : RoomDatabase() {

    // Define the DAO that will be used to interact with the satellites table
    abstract fun satelliteDao(): SatelliteDao

    companion object {
        @Volatile
        private var INSTANCE: SatelliteDatabase? = null

        // Singleton pattern to prevent multiple instances of the database being opened
        fun getDatabase(context: Context): SatelliteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SatelliteDatabase::class.java,
                    "satellite_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
