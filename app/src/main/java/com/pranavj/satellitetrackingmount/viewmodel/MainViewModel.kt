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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlin.math.*
import org.hipparchus.util.FastMath
import org.orekit.bodies.GeodeticPoint
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.time.TimeScalesFactory
import java.lang.Math.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val logs: StateFlow<List<String>> = AppLogger.logs

    private val satelliteRepository = SatelliteRepository(application)
    private lateinit var uartManager: UartManager

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    private val _streamingSatelliteName = MutableStateFlow<String?>(null)
    val streamingSatelliteName: StateFlow<String?> = _streamingSatelliteName

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

    //SATELLITE LIST SORTING CODE:
    data class SatelliteWithDistance(
        val satellite: Satellite,
        val startLat: Double,
        val startLon: Double,
        val distanceToUser: Double
    )

    private val _sortedSatellites = MutableStateFlow<List<Satellite>>(emptyList())
    val sortedSatellites: StateFlow<List<Satellite>> = _sortedSatellites.asStateFlow()

    fun sortSatellitesByDistance() {
        AppLogger.log("Test", "Sorting satellites...")
        viewModelScope.launch(Dispatchers.IO) {
            val userLocation = userTopocentricFrame.value
            if (userLocation == null) {
                AppLogger.log("Satellite Sorting", "User location is null. Cannot sort satellites.")  // ✅ Log failure case
                _sortedSatellites.value = _satellites.value  // Leave unsorted if no user location
                return@launch
            }

            val userLat = Math.toDegrees(userLocation.point.latitude)
            val userLon = Math.toDegrees(userLocation.point.longitude)
            Log.d("Satellite Sorting", "User location: Lat: $userLat, Lon: $userLon")
            AppLogger.log("Satellite Sorting", "User location: Lat: $userLat, Lon: $userLon")


            val sortedList = _satellites.value.mapNotNull { satellite ->

                val tle = TLE(satellite.line1, satellite.line2)
                val tleEpoch = tle.date.toDate(TimeScalesFactory.getUTC())
//                AppLogger.log("SortDebug", "Satellite: ${satellite.name} | TLE Epoch: $tleEpoch")

                val path = SatellitePropagator.generateLatLonPath(
                    satellite.line1, satellite.line2,
                    SatellitePropagator.getCurrentStartDate(),
                    duration = 300.0, stepSize = 1.0
                )

                val startPoint = path.getOrNull(300)



                if (startPoint == null) {
                    Log.d("Satellite Sorting", "No valid future position for satellite: ${satellite.name}")
                    AppLogger.log("Satellite Sorting", "No valid future position for satellite: ${satellite.name}")  // ✅ Debugging log
                    return@mapNotNull null
                }

                val startLat = startPoint.first
                val startLon = startPoint.second
//                Log.d("SortDebug", "StartPoint = ($startLat, $startLon), User = ($userLat, $userLon)")
//                AppLogger.log("SortDebug", "StartPoint = ($startLat, $startLon), User = ($userLat, $userLon)")

                val distance = calculateHaversineDistance(startLat, startLon, userLat, userLon)



                Log.d("Satellite Sorting", "Satellite: ${satellite.name} | Start Lat: $startLat, Start Lon: $startLon | Distance: $distance km")
                AppLogger.log(
                    "Satellite Sorting",
                    "Satellite: ${satellite.name} | Start Lat: $startLat, Start Lon: $startLon | Distance: $distance km"
                )

                SatelliteWithDistance(satellite, startLat, startLon, distance)
            }

            val finalSortedList = sortedList.sortedBy { it.distanceToUser }

            if (sortedList.isNotEmpty()) {
                _sortedSatellites.value = finalSortedList.map {it.satellite}

                AppLogger.log("Satellite Sorting", "Satellites (5 min in future) are sorted by least to greatest distance from user's location.")
            } else {
                AppLogger.log("Satellite Sorting", "Sorting failed: No satellites available.")
            }
        }
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0  // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
//        val dLon = Math.toRadians(lon2 - lon1)
        var dLon = Math.toRadians(lon2 - lon1)
        if (dLon > Math.PI) dLon -= 2 * Math.PI
        else if (dLon < -Math.PI) dLon += 2 * Math.PI

        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

//        val a = sin(dLat / 2).pow(2) + cos(radLat1) * cos(radLat2) * sin(dLon / 2).pow(2)
        val a = sin(dLat/2) * sin(dLat/2) + cos(radLat1) * cos(radLat2) * sin(dLon /2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (R * c)  // Distance in km
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
        Color.Yellow,
        Color(0xFFFFA500), // Orange
        Color(0xFF8A2BE2), // BlueViolet
        Color(0xFF00FA9A), // MediumSpringGreen (brighter, clearer)
        Color(0xFFB22222), // FireBrick (darker red)
        Color(0xFF40E0D0), // Turquoise (distinct from Cyan)
        Color(0xFFFF1493), // DeepPink (stronger than HotPink)
        Color(0xFF7CFC00), // LawnGreen (distinct from Chartreuse)
        Color(0xFF4169E1), // RoyalBlue (darker than DodgerBlue)
        Color(0xFFFFD700)  // Gold (brighter than GoldenRod)
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
        // Initialize user location
//        initializeUserLocation()
//        //fetch satellites, then sort them by distance to user
//        fetchAndInsertSatellites{
////            sortSatellitesByDistance()
//            viewModelScope.launch {
//                _satellites.collect {
//                    if (_satellites.value.isNotEmpty()){
//                        sortSatellitesByDistance()
//                    }
//                }
//            }
//        }
        uartManager = UartManager(application.applicationContext)

        //fetch satellites
        fetchAndInsertSatellites {
            _databaseReady.value = true
        }

        initializeUserLocation()

        viewModelScope.launch {
            combine(userTopocentricFrame, satellites) { location, satelliteList ->
                location != null && satelliteList.isNotEmpty()
            }.collect { ready ->
                if (ready) {
                    sortSatellitesByDistance()
                }
            }
        }
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

                _isStreaming.value = true
                _streamingSatelliteName.value = satellite.name


                val timeStepMillis = 3000L

                val tle = TLE(satellite.line1, satellite.line2)
                val propagator = TLEPropagator.selectExtrapolator(tle)


                val azElFlow = flow {
                    while (true) {
                        val currentTime = SatellitePropagator.getCurrentStartDate()
                        val pvCoordinates = propagator.getPVCoordinates(currentTime, userLocation.parentShape.bodyFrame)

                        val azimuthRad = userLocation.getAzimuth(pvCoordinates.position, userLocation.parentShape.bodyFrame, currentTime)
                        val elevationRad = userLocation.getElevation(pvCoordinates.position, userLocation.parentShape.bodyFrame, currentTime)


                        val azimuthCW = Math.toDegrees(azimuthRad)
                        val azimuthCCW = (360 - azimuthCW) % 360 //convert to CCW
                        val elevationDeg = Math.toDegrees(elevationRad)
//                        AppLogger.log("StreamTest", "Azimuth CCW: ${azimuthCCW}°, Elevation: ${elevationDeg}°")

                        emit(Pair(azimuthCCW, elevationDeg))

                        delay(timeStepMillis) //ERROR
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
        _isStreaming.value = false
        _streamingSatelliteName.value = null
        uartManager.stopStreaming()

        //reset az/el offsets
        updateAzimuthOffset(0.0)
        updateElevationOffset(0.0)
    }

    fun updateAzimuthOffset(offset: Double) {
        AppLogger.log("Offset", "New Azimuth Offset = $offset")
        uartManager.sendAzimuthOffset(offset)
    }

    fun updateElevationOffset(offset: Double) {
        AppLogger.log("Offset", "New Elevation Offset = $offset")
        uartManager.sendElevationOffset(offset)
    }
}