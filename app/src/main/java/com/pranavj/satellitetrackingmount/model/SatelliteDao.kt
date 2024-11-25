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

    // Fetch all TLE lines from the database
    @Query("SELECT line1, line2 FROM satellites")
    suspend fun getAllTLELines(): List<TLELines>
    // Data class to hold line1 and line2
    data class TLELines(val line1: String, val line2: String)

    // Fetch a satellite by Norad Catalog Number
    @Query("SELECT * FROM satellites WHERE noradCatalogNumber = :noradId LIMIT 1")
    suspend fun getSatelliteByNoradId(noradId: Int): Satellite
}
