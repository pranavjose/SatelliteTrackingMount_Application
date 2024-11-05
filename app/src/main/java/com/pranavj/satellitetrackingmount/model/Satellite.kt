package com.pranavj.satellitetrackingmount.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Define the Satellite entity that maps to the "satellites" table in the database
@Entity(tableName = "satellites")
data class Satellite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,  // Auto-generated primary key for each satellite
    val internationalDesignator: String,  // Example: "1998-067A"
    val noradCatalogNumber: Int,  // NORAD catalog number
    val name: String,  // Name of the satellite
    val periodMinutes: Double,  // Orbital period in minutes
    val inclinationDegrees: Double,  // Inclination of the orbit in degrees
    val apogeeHeightKm: Double,  // Apogee height (in kilometers)
    val perigeeHeightKm: Double,  // Perigee height (in kilometers)
    val eccentricity: Double,  // Eccentricity of the orbit
    val line1: String,  // First line of the TLE
    val line2: String   // Second line of the TLE
)
