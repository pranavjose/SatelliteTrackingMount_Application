package com.pranavj.satellitetrackingmount

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
//import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
//import androidx.lifecycle.lifecycleScope
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.MapOptions
//import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
//import com.pranavj.satellitetrackingmount.utils.OrekitInitializer
//import com.pranavj.satellitetrackingmount.utils.SatellitePropagator
//import com.pranavj.satellitetrackingmount.utils.UserLocationManager
//import kotlinx.coroutines.launch
import com.pranavj.satellitetrackingmount.viewmodel.MainViewModel
import com.mapbox.maps.plugin.annotation.annotations
//import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.pranavj.satellitetrackingmount.model.PathMetadata
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.ui.SatelliteListPage
import com.pranavj.satellitetrackingmount.utils.AppLogger
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RequestLocationPermission(
                onPermissionGranted = {

                      AppContent(mainViewModel)

                },
                onPermissionDenied = {
                    NoPermissionContent()
                }
            )
        }
    }

    @Composable
    fun NoPermissionContent() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Permission is required to access your location.")
        }
    }
}

@Composable
fun AppContent(mainViewModel: MainViewModel) {
    // Observe database and user location readiness
    val databaseReady by mainViewModel.databaseReady.collectAsState()
    val userLocationReady by mainViewModel.userTopocentricFrame.collectAsState()

    if (!databaseReady || userLocationReady == null) {
        // Show a global loading indicator
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(
                    text = "Initializing app...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    } else {
        // Load the navigation graph once everything is ready
        NavigationGraph(mainViewModel)
    }
}




@Composable
fun MapScreen(userLongitude: Double, userLatitude: Double, mainViewModel: MainViewModel) {
    val satellitePaths by mainViewModel.satellitePaths.collectAsState()
    val refreshMap by mainViewModel.refreshMap.collectAsState() //refresh map trigger from clear paths fn
    // Retrieve context in a composable-safe way
    val context = LocalContext.current
    // Convert radians to degrees for Mapbox
    val longitudeInDegrees = Math.toDegrees(userLongitude)
    val latitudeInDegrees = Math.toDegrees(userLatitude)
    //Log.d("MapScreen", "Longitude (radians): $userLongitude, Latitude (radians): $userLatitude")
    Log.d("MapScreen", "Final values: Longitude (degrees): $longitudeInDegrees, Latitude (degrees): $latitudeInDegrees")
    //AppLogger.log("MapScreen", "Final values: Longitude (degrees): $longitudeInDegrees, Latitude (degrees): $latitudeInDegrees")


    // Configure MapInitOptions
    val mapInitOptions = remember {
        MapInitOptions(
            context = context,
            mapOptions = MapOptions.Builder().build(), // Basic MapOptions setup
            cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(longitudeInDegrees, latitudeInDegrees)) // Longitude, Latitude
                .zoom(6.0) // Default zoom level
                .build(),
            styleUri = Style.MAPBOX_STREETS // Set the default style

        )
    }

    // Initialize MapView
    val mapView = remember { MapView(context, mapInitOptions) }
    val pointAnnotationManager = remember {mapView.annotations.createPointAnnotationManager()}
    val polylineAnnotationManager = remember {mapView.annotations.createPolylineAnnotationManager()}


    // Handle the clear command
    LaunchedEffect(Unit) {
        mainViewModel.clearCommand.collect {
            Log.d("MapScreen", "Received clear command. Clearing annotations.")
            AppLogger.log("MapScreen", "Received clear command. Clearing annotations.")
            polylineAnnotationManager.deleteAll()
            pointAnnotationManager.deleteAll()
            Log.d("PointAnnotationManager", "Clearing manager: ${pointAnnotationManager.hashCode()}")
        }
    }
    // Display the MapView in Compose
    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    ) {
        // Use the new `subscribeStyleLoaded` method
        val mapboxMap = mapView.mapboxMap

        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(longitudeInDegrees,latitudeInDegrees))
                .zoom(6.0)
                .build()
        )
        mapboxMap.subscribeStyleLoaded { _ ->
            mapboxMap.getStyle { style ->
                // Add user location marker
                val userLocationPoint = Point.fromLngLat(longitudeInDegrees, latitudeInDegrees)
                // Load and scale marker icon for the satellite's start and stop location

                val satBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.satellite_icon)
                val scaledBitmapSAT = Bitmap.createScaledBitmap(satBitmap, 144, 144, false)

                style.addImage("satellite_icon", scaledBitmapSAT)

                addMarker(mapView, userLocationPoint, "You")



                satellitePaths.forEach { (_, metadata) ->
                    // Plot each path
                    val polylineOptions = PolylineAnnotationOptions()
                        .withPoints(metadata.pathPoints)
                        .withLineWidth(4.0)
                        .withLineColor(metadata.color)
                    polylineAnnotationManager.create(polylineOptions)

                    // add start and stop markers

                    pointAnnotationManager.create(PointAnnotationOptions()
                        .withPoint(metadata.startMarker)
                        .withIconImage("satellite_icon")
                        .withTextField("Start")
                        .withTextSize(30.0)
                        .withTextOffset(listOf(0.0,3.0))
                        .withTextHaloColor("#FFFFFF")
                        .withTextHaloWidth(3.0)
                    )

                    pointAnnotationManager.create(PointAnnotationOptions()
                        .withPoint(metadata.stopMarker)
                        .withIconImage("satellite_icon")
                        .withTextField("Stop")
                        .withTextSize(30.0)
                        .withTextOffset(listOf(0.0,3.0))
                        .withTextHaloColor("#FFFFFF")
                        .withTextHaloWidth(3.0)
                    )

                }
            }
        }
    }
}


private fun addMarker(mapView: MapView, point: Point, title: String) {
    val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
//    Log.d("PointAnnotationManager", "Using manager: ${pointAnnotationManager.hashCode()} for marker: $title")

//    val markerBitmap = BitmapFactory.decodeResource(mapView.context.resources, R.drawable.marker_icon)
//    val scaledBitmapSAT = Bitmap.createScaledBitmap(markerBitmap, 192, 192, false)
    val markerBitmap = BitmapFactory.decodeResource(mapView.context.resources, R.drawable.marker_icon)
    val scaledBitmap = Bitmap.createScaledBitmap(markerBitmap, 96, 96, false)

    pointAnnotationManager.create(
        PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(scaledBitmap)
            .withTextField(title)
            .withTextSize(30.0)
            .withTextOffset(listOf(0.0, 2.0))
            .withTextHaloColor("#FFFFFF")
            .withTextHaloWidth(3.0)
    )
}


@Composable
fun RequestLocationPermission(onPermissionGranted: @Composable () -> Unit, onPermissionDenied: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            permissionGranted = granted
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionGranted = true
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (permissionGranted) {
        onPermissionGranted()
    } else {
        onPermissionDenied()
    }
}

@Composable
fun NavigationGraph(mainViewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "map_screen") {
        composable("map_screen") {
            MapScreenWithNavigation(navController, mainViewModel)
        }
        composable("satellite_list") {
            SatelliteListPage(mainViewModel, navController)
        }
        composable("log_page"){
            LogPage(navController)
        }
    }
}

@Composable
fun MapScreenWithNavigation(navController: NavHostController, mainViewModel: MainViewModel) {
    // Observe the user's location from the ViewModel
    val userTopocentricFrame by mainViewModel.userTopocentricFrame.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    // Simulate a loading delay if necessary
    LaunchedEffect(userTopocentricFrame) {
        if (userTopocentricFrame == null) {
            isLoading = true
        } else {
            // Introduce a slight delay for smoother transition (optional)
            kotlinx.coroutines.delay(300)
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // Show a loading indicator while waiting for the user's location
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (userTopocentricFrame != null) {
            // Render the map only after the user's location is available
            MapScreen(
                userLongitude = userTopocentricFrame!!.point.longitude,
                userLatitude = userTopocentricFrame!!.point.latitude,
                mainViewModel = mainViewModel
            )
        }

        // Overlay the legend
        SatelliteLegendDropdown(
            satellitePaths = mainViewModel.satellitePaths.collectAsState().value
        )

        // Add a button to navigate to the Satellite List Page
        Button(
            onClick = { navController.navigate("satellite_list") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("View Satellite List")
        }
        Button(
            onClick = { mainViewModel.clearAllPaths() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Clear All")
        }
        // Add a button to navigate to the Log Page
        Button(
            onClick = { navController.navigate("log_page") },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Log")
        }

    }
}

@Composable
fun SatelliteLegendDropdown(
    satellitePaths: Map<Satellite, PathMetadata>
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Dropdown toggle button
        Button(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide Legend" else "Show Legend")
        }

        // Conditionally show the legend items
        if (expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Currently Plotted Satellites", style = MaterialTheme.typography.h6)
                satellitePaths.forEach { (satellite, metadata) ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(metadata.color))
                        )
                        Text(
                            text = satellite.name,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogPage(navController: NavHostController) {
    val logs = remember { AppLogger.getLogs() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(logs) { log ->
            Text(
                text = log,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
    Button(onClick = { navController.popBackStack() }) {
        Text("Back")
    }
}




