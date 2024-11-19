package com.pranavj.satellitetrackingmount

//import org.mapsforge.map.view.MapView
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import com.pranavj.satellitetrackingmount.ui.SatelliteListActivity
import com.pranavj.satellitetrackingmount.utils.SatellitePropagator
import com.pranavj.satellitetrackingmount.utils.UserLocationManager
import com.pranavj.satellitetrackingmount.utils.OrekitInitializer
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Orekit
        OrekitInitializer.initializeOrekit(this)

        // Create an instance of the SatelliteRepository
        val satelliteRepository = SatelliteRepository(this)
        val satellitePropagator = SatellitePropagator()
        // Create an instance of UserLocationManager
        val userLocationManager = UserLocationManager()

        // Create the user's location TopocentricFrame
        val userTopocentricFrame = userLocationManager.createUserLocation()

        // Insert data into the database (for testing purposes)
        lifecycleScope.launch {
            val satellites = satelliteRepository.getSatellitesFromTLE() // Fetches satellite data (assuming this is implemented)
            satelliteRepository.insertSatellites(satellites) // Insert the data
            Log.d("MainActivity", "Inserted ${satellites.size} satellites.")
        }

        // Log the details to verify the function works as intended
        Log.d("UserLocationTest", "User Topocentric Frame created: ${userTopocentricFrame.name}")

        // Initialize the AndroidGraphicFactory
        AndroidGraphicFactory.createInstance(application)


        // Initialize mapView from the layout
        mapView = findViewById(R.id.mapView)

        // Configure the MapView
        mapView.mapScaleBar.isVisible = true
        mapView.setBuiltInZoomControls(true)

        // Create and configure the MapView
//        mapView = MapView(this).also {
//            it.mapScaleBar.isVisible = true
//            it.setBuiltInZoomControls(true)
//        }

        // Set the MapView as the content view
//        setContentView(mapView)

        // In your onCreate() method or wherever needed
        val tileCache: TileCache = AndroidUtil.createTileCache(
            this,
            "mapcache",
            mapView.model.displayModel.tileSize,
            1f,
            mapView.model.frameBufferModel.overdrawFactor
        )

        try {
            // Load the .map file from the assets folder and copy it to a temporary file
            val inputStream: InputStream = assets.open("mapsforge/world.map")
            val tempFile = File.createTempFile("world", ".map", cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
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

        // Button to navigate to SatelliteListActivity
        val btnViewSatellites: Button = findViewById(R.id.btnViewSatellites)
        btnViewSatellites.setOnClickListener {
            val intent = Intent(this, SatelliteListActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.destroyAll() // Clean up MapView resources
        AndroidGraphicFactory.clearResourceMemoryCache() // Clear resource cache for graphics
    }
}
