package com.pranavj.satellitetrackingmount.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import org.orekit.bodies.GeodeticPoint
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.frames.FramesFactory
import org.orekit.frames.TopocentricFrame
import org.orekit.utils.Constants
import org.orekit.utils.IERSConventions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
class UserLocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun fetchRealUserLocation(onLocationAvailable: (TopocentricFrame) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitudeRadians = Math.toRadians(location.latitude)
                val longitudeRadians = Math.toRadians(location.longitude)
                val altitude = location.altitude

                Log.d("UserLocationManager", "Real Location: lat=${location.latitude}, lon=${location.longitude}, alt=$altitude")

                // Convert to Orekit's GeodeticPoint and TopocentricFrame
                val geodeticPoint = GeodeticPoint(latitudeRadians, longitudeRadians, altitude)
                val earthShape = OneAxisEllipsoid(
                    Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    FramesFactory.getITRF(IERSConventions.IERS_2010, true)
                )
                val userTopocentricFrame = TopocentricFrame(earthShape, geodeticPoint, "User Location")

                // Pass the TopocentricFrame back
                onLocationAvailable(userTopocentricFrame)
            } else {
                Log.e("UserLocationManager", "Failed to fetch location.")
            }
        }
    }
}