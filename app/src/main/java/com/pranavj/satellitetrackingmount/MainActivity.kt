package com.pranavj.satellitetrackingmount

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import androidx.lifecycle.lifecycleScope
import com.pranavj.satellitetrackingmount.model.SatelliteDatabase
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create an instance of the SatelliteRepository
        val satelliteRepository = SatelliteRepository(this)

        // Launch a coroutine to handle database operations
        lifecycleScope.launch {
            val satellites = satelliteRepository.getSatellitesFromTLE()

            // Insert parsed satellites into the database asynchronously
            satelliteRepository.insertSatellites(satellites)

            // Fetch and log all satellites from the database to verify insertion
            val satelliteDao = SatelliteDatabase.getDatabase(this@MainActivity).satelliteDao()
            val storedSatellites = satelliteDao.getAllSatellites()


            // Log the satellite details to verify successful insertion
            storedSatellites.forEach {
                Log.d("Satellite", """ 
                    Name: ${it.name}
                    NORAD Catalog Number: ${it.noradCatalogNumber}
                    International Designator: ${it.internationalDesignator}
                    Period (minutes): ${it.periodMinutes}
                    Inclination (degrees): ${it.inclinationDegrees}
                    Apogee Height (km): ${it.apogeeHeightKm}
                    Perigee Height (km): ${it.perigeeHeightKm}
                    Eccentricity: ${it.eccentricity}
                    Line 1: ${it.line1}
                    Line 2: ${it.line2}
                """.trimIndent())
            }
        }

    }
}
