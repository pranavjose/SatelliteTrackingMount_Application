package com.pranavj.satellitetrackingmount

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import com.pranavj.satellitetrackingmount.utils.SatellitePropagator
import com.pranavj.satellitetrackingmount.utils.OrekitInitializer
import com.pranavj.satellitetrackingmount.utils.UserLocationManager
import kotlinx.coroutines.launch
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.time.AbsoluteDate
import org.orekit.time.TimeScalesFactory
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Orekit
        OrekitInitializer.initializeOrekit(this)

        // Create an instance of the SatelliteRepository
        val satelliteRepository = SatelliteRepository(this)
        val satellitePropagator = SatellitePropagator()
        // Create an instance of UserLocationManager
        val userLocationManager = UserLocationManager()

        // Create the user's location TopocentricFrame
        val userTopocentricFrame = userLocationManager.createUserLocation()

        // Log the details to verify the function works as intended
//        Log.d("UserLocationTest", "User Topocentric Frame created: ${userTopocentricFrame.name}")



        // Define a sample start date for propagation
        val utc = TimeScalesFactory.getUTC()
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.NOVEMBER, 10, 12, 0, 0) // Example start date: November 10, 2024, 12:00:00 UTC
        val startDate = AbsoluteDate(calendar.time, utc)

        // Launch a coroutine to generate and log the satellite path
        lifecycleScope.launch {
            try {
                // Retrieve TLE objects from the repository
                val tleObjects = satelliteRepository.getTLEObjects()

                if (tleObjects.isNotEmpty()) {
                    tleObjects.forEach { tle ->
                        // Generate a path for 1 hour with 1-minute intervals (example values)
                        val path = satellitePropagator.generateAzimuthElevationPath(
                            tle,
                            userTopocentricFrame,
                            startDate,
                            durationSeconds = 3600.0, // Total duration: 1 hour
                            stepSeconds = 60.0 // Step interval: 1 minute
                        )

                        // Log the generated path
                        Log.d("SatellitePath", "Path for Satellite TLE: ${tle.line1} | ${tle.line2}")
                        path.forEachIndexed { index, (azimuth, elevation) ->
                            Log.d("SatellitePath", "Step $index - Azimuth: $azimuth degrees, Elevation: $elevation degrees")
                        }
                    }
                } else {
                    Log.d("MainActivity", "No TLE objects found in the database.")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error generating satellite path: ${e.message}")
            }
        }

    }
}
