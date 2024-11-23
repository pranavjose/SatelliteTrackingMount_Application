package com.pranavj.satellitetrackingmount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.MapOptions
//import com.mapbox.maps.plugin.Plugin
//import com.mapbox.maps.plugin.attribution.AttributionPlugin
//import com.mapbox.maps.plugin.gestures.GesturesPlugin
//import com.mapbox.maps.plugin.compass.CompassPlugin
//import com.mapbox.maps.plugin.logo.LogoPlugin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapScreen()
        }
    }
}
@Composable
fun MapScreen() {
    // Retrieve context in a composable-safe way
    val context = LocalContext.current

    // Configure MapInitOptions
    val mapInitOptions = remember {
        MapInitOptions(
            context = context,
            mapOptions = MapOptions.Builder().build(), // Basic MapOptions setup
            cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(0.0, 0.0)) // Longitude, Latitude
                .zoom(2.0) // Default zoom level
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
    )
}
