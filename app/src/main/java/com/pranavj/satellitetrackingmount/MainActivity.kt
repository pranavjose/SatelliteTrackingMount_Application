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
import androidx.compose.material.Button
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.pranavj.satellitetrackingmount.ui.SatelliteListPage
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var mapView: MapView



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

//@Composable
//fun MainContent(mainViewModel: MainViewModel) {
//    // Collect the userTopocentricFrame as state
//    val userTopocentricFrameState = mainViewModel.userTopocentricFrame.collectAsState()
//    // Get the current value
//    val userTopocentricFrame = userTopocentricFrameState.value
//
//    if (userTopocentricFrame != null) {
//        // Safely use the frame
//        MapScreen(
//            userLongitude = userTopocentricFrame.point.longitude,
//            userLatitude = userTopocentricFrame.point.latitude
//        )
//    } else {
//        // Show a loading indicator while the user's location is being determined
//        Box(
//            modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                CircularProgressIndicator()
//                Text(text = "Loading user location...", modifier = Modifier.padding(top = 16.dp))
//            }
//        }
//    }
//}


@Composable
fun MapScreen(userLongitude: Double, userLatitude: Double, mainViewModel: MainViewModel) {
    val satellitePath by mainViewModel.satellitePath.collectAsState()
    // Retrieve context in a composable-safe way
    val context = LocalContext.current
    // Convert radians to degrees for Mapbox
    val longitudeInDegrees = Math.toDegrees(userLongitude)
    val latitudeInDegrees = Math.toDegrees(userLatitude)
    //Log.d("MapScreen", "Longitude (radians): $userLongitude, Latitude (radians): $userLatitude")
    Log.d("MapScreen", "Final values: Longitude (degrees): $longitudeInDegrees, Latitude (degrees): $latitudeInDegrees")


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
                val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
                pointAnnotationManager.deleteAll()
                // Add user location marker
                val userLocationPoint = Point.fromLngLat(longitudeInDegrees, latitudeInDegrees)
                // Load and scale marker icon for the user's location
                val markerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.marker_icon)
                val scaledBitmap = Bitmap.createScaledBitmap(markerBitmap, 96, 96, false)
                style.addImage("marker-icon", scaledBitmap)

                val userMarker = PointAnnotationOptions()
                    .withPoint(userLocationPoint)
                    .withIconImage("marker-icon")
                pointAnnotationManager.create(userMarker)

                // Plot the satellite path if available
                if (satellitePath.isNotEmpty()) {
                    val polylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager()
                    polylineAnnotationManager.deleteAll()//clear old polylines
                    // Add a debug log for each latitude and longitude point
                    satellitePath.forEach { (lat, lon) ->
                        Log.d("SatellitePathPlot", "Latitude: $lat, Longitude: $lon")
                    }

                    val polylineOptions = PolylineAnnotationOptions()
                        .withPoints(satellitePath.map { (lat, lon) -> Point.fromLngLat(lon, lat) }) // Destructure Pair
                        .withLineWidth(4.0)
                        .withLineColor("#FF0000") // Example color: red
                    polylineAnnotationManager.create(polylineOptions)
                    Log.d("MapScreen", "Plotted satellite path with ${satellitePath.size} points.")
                }

            }

            // Create a MapFile using the temporary file
            val mapFile = MapFile(tempFile)

            // Create a TileRendererLayer to display the map
            val tileRendererLayer = TileRendererLayer(
                tileCache,
                mapFile,
                mapView.model.mapViewPosition,
                AndroidGraphicFactory.INSTANCE
            ).apply {
                setXmlRenderTheme(org.mapsforge.map.rendertheme.InternalRenderTheme.DEFAULT)
            }

            // Add the TileRendererLayer to the MapView
            mapView.layerManager.layers.add(tileRendererLayer)

            // Set initial map position and zoom level
            mapView.model.mapViewPosition.mapPosition = MapPosition(LatLong(0.0, 0.0), 2)



        } catch (e: Exception) {
            // Handle the exception
            e.printStackTrace()
        }
    }
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

        // Add a button to navigate to the Satellite List Page
        Button(
            onClick = { navController.navigate("satellite_list") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("View Satellite List")
        }
    }
}


