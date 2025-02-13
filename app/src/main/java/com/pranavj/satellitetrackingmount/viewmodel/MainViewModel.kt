package com.pranavj.satellitetrackingmount.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.pranavj.satellitetrackingmount.model.PathMetadata
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import com.pranavj.satellitetrackingmount.utils.AppLogger
import com.pranavj.satellitetrackingmount.utils.SatellitePropagator
import com.pranavj.satellitetrackingmount.utils.OrekitInitializer
import com.pranavj.satellitetrackingmount.utils.UartManager
import com.pranavj.satellitetrackingmount.utils.UserLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hipparchus.util.FastMath
import org.orekit.bodies.GeodeticPoint
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val satelliteRepository = SatelliteRepository(application)
    private lateinit var uartManager: UartManager

    fun requestUsbAccess(){
        AppLogger.log("MainViewModel", "Requesting USB access...")
        uartManager.requestUsbPermission()
    }

    fun openSerialPort(): Boolean{
        AppLogger.log("MainViewModel", "Opening serial connection...")
        return uartManager.openSerialConnection()
    }

    fun checkUsbPermission(): Boolean {
        return uartManager.isUsbPermissionGranted()
    }

    // StateFlow to hold the list of satellites
    private val _satellites = MutableStateFlow<List<Satellite>>(emptyList())
    val satellites = _satellites.asStateFlow()

    // StateFlow to hold the user's TopocentricFrame
    private val _userTopocentricFrame = MutableStateFlow<TopocentricFrame?>(null)
    val userTopocentricFrame = _userTopocentricFrame.asStateFlow()

    private val _databaseReady = MutableStateFlow(false)
    val databaseReady = _databaseReady.asStateFlow()


    private val userLocationManager = UserLocationManager(application.applicationContext)

    private val _refreshMap = MutableStateFlow(false)
    val refreshMap: StateFlow<Boolean> = _refreshMap

    private val _clearCommand = MutableSharedFlow<Unit>()
    val clearCommand = _clearCommand.asSharedFlow()

    private val _duration = MutableStateFlow("5") // Default value as a string
    val duration: StateFlow<String> = _duration

    private val _stepSize = MutableStateFlow("0.5") // Default value as a string
    val stepSize: StateFlow<String> = _stepSize

    private val pathColors = listOf(
        Color.Red,
        Color.Blue,
        Color.Green,
        Color.Cyan,
        Color.Magenta,
        Color.Yellow
    )

    //private val _satellitePaths = MutableLiveData<Map<Satellite, PathMetadata>>()
    //val satellitePaths: LiveData<Map<Satellite, PathMetadata>> = _satellitePaths
    private var colorIndex = 0

    private fun getNextColor(): Int {
        val color = pathColors[colorIndex]
        colorIndex = (colorIndex + 1) % pathColors.size // Cycle through the colors
        return android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }


    fun updateDuration(newDuration: String) {
        _duration.value = newDuration
        AppLogger.log("Az/EL Propagation Settings", "New Propagation Duration = $newDuration")
    }

    fun updateStepSize(newStepSize: String) {
        _stepSize.value = newStepSize
        AppLogger.log("Az/EL Propagation Settings", "New Propagation Step Size = $newStepSize")
    }

    init {
        // Initialize Orekit
        OrekitInitializer.initializeOrekit(application)
        initializeUserLocation()
        fetchAndInsertSatellites()
        uartManager = UartManager(application.applicationContext)
    }

    fun clearAllPaths() {
        _satellitePaths.value = emptyMap() // Clear all paths
//        _refreshMap.value = !_refreshMap.value
        viewModelScope.launch {
            _clearCommand.emit(Unit) // Emit the clear event
        }
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
                AppLogger.log("DatabaseTest", "Inserted ${fetchedSatellites.size} satellites.")
                delay(1000) // Debugging delay (1 second)
                // Fetch satellites after insertion
                val allSatellites = satelliteRepository.getAllSatellites()

                _satellites.value = allSatellites

                Log.d("SatelliteLog", "Fetched ${allSatellites.size} satellites.")
                AppLogger.log("SatelliteLog", "Fetched ${allSatellites.size} satellites.")
                allSatellites.forEach { satellite ->
                    Log.d("SatelliteLog", "ID: ${satellite.id}, Name: ${satellite.name}")
                    //AppLogger.log("SatelliteLog", "ID: ${satellite.id}, Name: ${satellite.name}")
                }

                // Trigger database readiness
                _databaseReady.value = true

                onComplete()
            } catch (e: Exception) {
                // Handle exceptions (e.g., logging, notifying the user)
                Log.e("DatabaseTest", "Error during satellite fetch/insert: ${e.message}")
                AppLogger.log("DatabaseTest", "Error during satellite fetch/insert: ${e.message}")
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


    //private val _satellitePath = MutableStateFlow<List<Pair<Double,Double>>>(emptyList())
    //val satellitePath = _satellitePath.asStateFlow()

    private val _satellitePaths = MutableStateFlow<Map<Satellite, PathMetadata>>(emptyMap())
    val satellitePaths = _satellitePaths.asStateFlow()

    fun plotSatellitePath(noradId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //clear old path
                //_satellitePath.value = emptyList()

                // Fetch satellite by NORAD ID
                val satellite = satelliteRepository.getSatelliteByNoradId(noradId)

                // Generate the latitude/longitude path
                val latLonPath = SatellitePropagator.generateLatLonPath(
                    satellite.line1,
                    satellite.line2,
                    startDate = SatellitePropagator.getCurrentStartDate(),
                    duration = 5400.0, // Fixed duration for now
                    stepSize = 60.0  // Fixed step size for now
                )
                if (latLonPath.isEmpty()) {
                    Log.e("SatellitePath", "Generated path is empty for NORAD ID $noradId.")
                    AppLogger.log("SatellitePath", "Generated path is empty for NORAD ID $noradId.")
                    return@launch
                } else {
                    Log.d("SatellitePath", "Path for ${satellite.name} generated with ${latLonPath.size} points.")
                    AppLogger.log("SatellitePath", "Path for ${satellite.name} generated with ${latLonPath.size} points.")
                }

                val pathPoints = latLonPath.map{ (lat, lon) -> Point.fromLngLat(lon, lat)}

                val color = getNextColor()

                val pathMetadata = PathMetadata(
                    pathPoints = pathPoints,
                    color = color,
                    startMarker = pathPoints.first(),
                    stopMarker = pathPoints.last()
                )

                // Emit the new path on the Main dispatcher
                launch(Dispatchers.Main) {
//                    _satellitePath.value = latLonPath
                    val currentPaths = _satellitePaths.value.toMutableMap()
                    currentPaths[satellite] = pathMetadata
                    _satellitePaths.value = currentPaths
                }
            } catch (e: Exception) {
                // Handle errors gracefully
                Log.e("SatellitePath", "Error generating path for NORAD ID $noradId: ${e.message}")
                AppLogger.log("SatellitePath", "Error generating path for NORAD ID $noradId: ${e.message}")
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
                    durationSeconds = 300.0, // User-defined settings can be added later
                    stepSeconds = 0.5, // User-defined settings can be added later
                    userTopocentricFrame = userLocation
                )

                // Log the result (placeholder for now)
                Log.d("SatelliteAzEl", "Azimuth/Elevation Path for ${satellite.name}: $azElPath")
                AppLogger.log("SatelliteAzEl", "Azimuth/Elevation Path for ${satellite.name}: $azElPath")

            } catch (e: Exception) {
                // Handle errors gracefully
                Log.e("SatelliteAzEl", "Error generating Az/El data for NORAD ID $noradId: ${e.message}")
                AppLogger.log("SatelliteAzEl", "Error generating Az/El data for NORAD ID $noradId: ${e.message}")
            }
        }
    }

    fun startAzElStreaming(noradId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val satellite = satelliteRepository.getSatelliteByNoradId(noradId)
                val userLocation = userTopocentricFrame.value ?: throw IllegalStateException("User location not set")

                val timeStepMillis = 5000L

                val azElFlow = flow {
                    var currentTime = SatellitePropagator.getCurrentStartDate()
                    while (true) {
                        val azElData = SatellitePropagator.generateAzimuthElevationPath(
                            satellite.line1,
                            satellite.line2,
                            currentTime,
                            durationSeconds = 300.0, // User-defined settings can be added later
                            stepSeconds = 5.0, // User-defined settings can be added later
                            userTopocentricFrame = userLocation
                        ).lastOrNull()

                        if (azElData != null){
                            emit(azElData)
                        }
                        delay(timeStepMillis) //ERROR
                        currentTime = currentTime.shiftedBy(timeStepMillis / 1000.0)
                    }
                }.flowOn(Dispatchers.IO)

                uartManager.startStreaming(azElFlow, timeStepMillis)
            } catch (e: Exception) {
                //TO DO
                AppLogger.log("SatelliteAzEl", "Error streaming Az/El: ${e.message}")
            }
        }
    }

    fun stopAzElStreaming() {
        uartManager.stopStreaming()
    }

}