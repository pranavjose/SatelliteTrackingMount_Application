package com.pranavj.satellitetrackingmount.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import com.pranavj.satellitetrackingmount.utils.OrekitInitializer
import com.pranavj.satellitetrackingmount.utils.UserLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.orekit.bodies.GeodeticPoint
import org.orekit.frames.TopocentricFrame

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val satelliteRepository = SatelliteRepository(application)

    // StateFlow to hold the list of satellites
    private val _satellites = MutableStateFlow<List<Satellite>>(emptyList())
    val satellites = _satellites.asStateFlow()

    // StateFlow to hold the user's TopocentricFrame
    private val _userTopocentricFrame = MutableStateFlow<TopocentricFrame?>(null)
    val userTopocentricFrame = _userTopocentricFrame.asStateFlow()

    init {
        // Initialize Orekit
        OrekitInitializer.initializeOrekit(application)
        initializeUserLocation()
        fetchAndInsertSatellites()
    }

    /**
     * Fetches satellites from TLE and inserts them into the database.
     */
    private fun fetchAndInsertSatellites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fetchedSatellites = satelliteRepository.getSatellitesFromTLE()
                satelliteRepository.insertSatellites(fetchedSatellites)

                // Update the StateFlow with the new list of satellites
                _satellites.value = satelliteRepository.getAllSatellites()
            } catch (e: Exception) {
                // Handle exceptions (e.g., logging, notifying the user)
                e.printStackTrace()
            }
        }
    }

    private fun initializeUserLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userLocationManager = UserLocationManager()
                val userTopocentricFrame = userLocationManager.createUserLocation()

                // Log the TopocentricFrame values
                val geodeticPoint = userTopocentricFrame.point
                println("TopocentricFrame: lat=${geodeticPoint.latitude}, lon=${geodeticPoint.longitude}, alt=${geodeticPoint.altitude}")

                // Update the StateFlow with the user's TopocentricFrame
                _userTopocentricFrame.value = userTopocentricFrame
            } catch (e: Exception) {
                // Handle exceptions (e.g., logging, notifying the user)
                e.printStackTrace()
            }
        }
    }
}