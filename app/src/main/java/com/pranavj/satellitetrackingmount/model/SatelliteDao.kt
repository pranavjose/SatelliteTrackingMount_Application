package com.pranavj.satellitetrackingmount.model

import androidx.room.*

// Data Access Object (DAO) for the Satellite entity
@Dao
interface SatelliteDao {

    // Insert a new satellite into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatellite(satellite: Satellite)

    // Fetch all satellites from the database
    @Query("SELECT * FROM satellites")
    suspend fun getAllSatellites(): List<Satellite>

    // Delete all satellites from the database
    @Query("DELETE FROM satellites")
    suspend fun deleteAllSatellites()
}
