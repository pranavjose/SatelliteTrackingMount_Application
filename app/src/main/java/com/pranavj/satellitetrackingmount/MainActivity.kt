package com.pranavj.satellitetrackingmount

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import com.pranavj.satellitetrackingmount.utils.OrekitInitializer
import com.pranavj.satellitetrackingmount.utils.UserLocationManager
import kotlinx.coroutines.launch
import org.orekit.propagation.analytical.tle.TLE

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Orekit
        OrekitInitializer.initializeOrekit(this)

        // Create an instance of UserLocationManager
        val userLocationManager = UserLocationManager()

        // Create the user's location TopocentricFrame
        val userTopocentricFrame = userLocationManager.createUserLocation()

        // Log the details to verify the function works as intended
        Log.d("UserLocationTest", "User Topocentric Frame created: ${userTopocentricFrame.name}")

        // Create an instance of the SatelliteRepository
//        val satelliteRepository = SatelliteRepository(this)

        // Launch a coroutine to test the creation of TLE objects
//        lifecycleScope.launch {
//            try {
//                // Retrieve the list of TLE objects
//                val tleObjects: List<TLE> = satelliteRepository.getTLEObjects()
//
//                // Log the results to verify the TLE creation
//                if (tleObjects.isNotEmpty()) {
//                    Log.d("MainActivity", "Successfully retrieved ${tleObjects.size} TLE objects.")
//                    tleObjects.forEach { tle ->
//                        Log.d("MainActivity", "TLE Object: Line 1 - ${tle.line1}, Line 2 - ${tle.line2}")
//                    }
//                } else {
//                    Log.d("MainActivity", "No TLE objects retrieved from the database.")
//                }
//            } catch (e: Exception) {
//                Log.e("MainActivity", "Error retrieving and creating TLE objects: ${e.message}")
//            }
//        }

    }
}
