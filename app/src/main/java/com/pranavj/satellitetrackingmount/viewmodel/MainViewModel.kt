package com.pranavj.satellitetrackingmount.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import com.pranavj.satellitetrackingmount.utils.SatellitePropagator
import com.pranavj.satellitetrackingmount.utils.OrekitInitializer
import com.pranavj.satellitetrackingmount.utils.UserLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hipparchus.util.FastMath
import org.orekit.bodies.GeodeticPoint
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val satelliteRepository = SatelliteRepository(application)

    // StateFlow to hold the list of satellites
    private val _satellites = MutableStateFlow<List<Satellite>>(emptyList())
    val satellites = _satellites.asStateFlow()

    // StateFlow to hold the user's TopocentricFrame
    private val _userTopocentricFrame = MutableStateFlow<TopocentricFrame?>(null)
    val userTopocentricFrame = _userTopocentricFrame.asStateFlow()

    private val _databaseReady = MutableStateFlow(false)
    val databaseReady = _databaseReady.asStateFlow()


    private val userLocationManager = UserLocationManager(application.applicationContext)
    init {
        // Initialize Orekit
        OrekitInitializer.initializeOrekit(application)
        initializeUserLocation()
        fetchAndInsertSatellites()
    }

    /**
     * Fetches satellites from TLE and inserts them into the database.
     */
    private fun fetchAndInsertSatellites(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fetchedSatellites = satelliteRepository.getSatellitesFromTLE()
                satelliteRepository.insertSatellites(fetchedSatellites)
                Log.d("DatabaseTest", "Inserted ${fetchedSatellites.size} satellites.")

                delay(1000) // Debugging delay (1 second)
                // Fetch satellites after insertion
                val allSatellites = satelliteRepository.getAllSatellites()

                _satellites.value = allSatellites

                Log.d("SatelliteLog", "Fetched ${allSatellites.size} satellites.")
                allSatellites.forEach { satellite ->
                    Log.d("SatelliteLog", "ID: ${satellite.id}, Name: ${satellite.name}")
                }

                // Trigger database readiness
                _databaseReady.value = true

                onComplete()
            } catch (e: Exception) {
                // Handle exceptions (e.g., logging, notifying the user)
                Log.e("DatabaseTest", "Error during satellite fetch/insert: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun initializeUserLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            userLocationManager.fetchRealUserLocation { userTopocentricFrame ->
                _userTopocentricFrame.value = userTopocentricFrame
            }
        }
    }


    private val _satellitePath = MutableStateFlow<List<Pair<Double,Double>>>(emptyList())
    val satellitePath = _satellitePath.asStateFlow()
    fun plotSatellitePath(noradId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //clear old path
                _satellitePath.value = emptyList()
                // Fetch satellite by NORAD ID
                val satellite = satelliteRepository.getSatelliteByNoradId(noradId)

                // Generate the latitude/longitude path
                val latLonPath = SatellitePropagator.generateLatLonPath(
                    satellite.line1,
                    satellite.line2,
                    startDate = SatellitePropagator.getCurrentStartDate(),
                    duration = 5400.0, // Fixed duration for now
                    stepSize = 30.0  // Fixed step size for now
                )
                if (latLonPath.isEmpty()) {
                    Log.e("SatellitePath", "Generated path is empty for NORAD ID $noradId.")
                } else {
                    Log.d("SatellitePath", "Path for ${satellite.name} generated with ${latLonPath.size} points.")
                }

                // Emit the new path on the Main dispatcher
                launch(Dispatchers.Main) {
                    _satellitePath.value = latLonPath
                }
            } catch (e: Exception) {
                // Handle errors gracefully
                Log.e("SatellitePath", "Error generating path for NORAD ID $noradId: ${e.message}")
            }
        }
    }

    fun generateAzimuthElevation(noradId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch satellite by NORAD ID
                val satellite = satelliteRepository.getSatelliteByNoradId(noradId)

                // Ensure the user location is available
                val userLocation = userTopocentricFrame.value ?: throw IllegalStateException("User location not set")

                // Generate azimuth/elevation data
                val azElPath = SatellitePropagator.generateAzimuthElevationPath(
                    satellite.line1,
                    satellite.line2,
                    startDate = SatellitePropagator.getCurrentStartDate(),
                    durationSeconds = 10.0, // User-defined settings can be added later
                    stepSeconds = 30.0, // User-defined settings can be added later
                    userTopocentricFrame = userLocation
                )

                // Log the result (placeholder for now)
                Log.d("SatelliteAzEl", "Azimuth/Elevation Path for ${satellite.name}: $azElPath")
            } catch (e: Exception) {
                // Handle errors gracefully
                Log.e("SatelliteAzEl", "Error generating Az/El data for NORAD ID $noradId: ${e.message}")
            }
        }
    }


}