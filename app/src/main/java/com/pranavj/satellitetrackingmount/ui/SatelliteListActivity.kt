package com.pranavj.satellitetrackingmount.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pranavj.satellitetrackingmount.R
import com.pranavj.satellitetrackingmount.adapter.SatelliteAdapter
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import com.pranavj.satellitetrackingmount.utils.SatellitePropagator
import com.pranavj.satellitetrackingmount.utils.UserLocationManager
import kotlinx.coroutines.launch
import org.orekit.time.AbsoluteDate
import org.orekit.time.TimeScalesFactory
import java.util.Calendar


class SatelliteListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_satellite_list)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewSatellites)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val repository = SatelliteRepository(applicationContext)

        lifecycleScope.launch {
            val satellites = repository.getAllSatellites() // Fetching data
            Log.d("SatelliteListActivity", "Number of satellites fetched: ${satellites.size}")
            val adapter = SatelliteAdapter(satellites) {tle ->
                propagatePathForSatellite(tle)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun propagatePathForSatellite(tle: org.orekit.propagation.analytical.tle.TLE) {
        // Create instances for propagation if needed
        val satellitePropagator = SatellitePropagator()
        val userLocationManager = UserLocationManager()
        val userTopocentricFrame = userLocationManager.createUserLocation()

        // Define a sample start date for propagation
        val utc = TimeScalesFactory.getUTC()
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.NOVEMBER, 19, 22, 30, 0) // Example start date
        val startDate = AbsoluteDate(calendar.time, utc)

        lifecycleScope.launch {
            try {
                // Generate a path (1 hour duration, 1-minute intervals as an example)
                val path = satellitePropagator.generateAzimuthElevationPath(
                    tle,
                    userTopocentricFrame,
                    startDate,
                    durationSeconds = 3600.0, // Total duration: 1 hour
                    stepSeconds = 60.0 // Step interval: 1 minute
                )
                // Example: Display a summary result in a Toast
                val firstStep = path.firstOrNull()
                val resultMessage = if (firstStep != null) {
                    "First Step - Azimuth: ${firstStep.first} degrees, Elevation: ${firstStep.second} degrees"
                } else {
                    "No path generated"
                }
                Toast.makeText(this@SatelliteListActivity, resultMessage, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@SatelliteListActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}