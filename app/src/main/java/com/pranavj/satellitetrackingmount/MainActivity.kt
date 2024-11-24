package com.pranavj.satellitetrackingmount

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
//import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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


class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            //MapScreen()
            MainContent(mainViewModel)
        }
    }
}

@Composable
fun MainContent(mainViewModel: MainViewModel) {
    // Collect the userTopocentricFrame as state
    val userTopocentricFrameState = mainViewModel.userTopocentricFrame.collectAsState()

    // Get the current value
    val userTopocentricFrame = userTopocentricFrameState.value

    if (userTopocentricFrame != null) {
        // Safely use the frame
        MapScreen(
            userLongitude = userTopocentricFrame.point.longitude,
            userLatitude = userTopocentricFrame.point.latitude
        )
    } else {
        // Show a loading indicator while the user's location is being determined
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(text = "Loading user location...", modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}


@Composable
fun MapScreen(userLongitude: Double, userLatitude: Double) {
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
                .zoom(10.0) // Default zoom level
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
                .zoom(10.0)
                .build()
        )
        mapboxMap.subscribeStyleLoaded { _ ->
            // Add a static marker for the user's location after the style is loaded
            mapboxMap.getStyle { style ->
                // Load the marker icon from drawable and scale it
                val markerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.marker_icon)
                val scaledBitmap = Bitmap.createScaledBitmap(markerBitmap, 192, 192, false) // Adjust size as needed
                style.addImage("marker-icon", scaledBitmap)
            }
            val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

            val userLocationPoint = Point.fromLngLat(longitudeInDegrees, latitudeInDegrees)
            Log.d("MapScreen", "Creating marker at: $userLocationPoint")

            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(userLocationPoint)
                .withIconImage("marker-icon") // Replace with your custom marker image

            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }
}
