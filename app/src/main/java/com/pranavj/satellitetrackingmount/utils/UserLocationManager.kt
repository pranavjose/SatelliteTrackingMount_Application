package com.pranavj.satellitetrackingmount.utils

import org.orekit.bodies.GeodeticPoint
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.frames.FramesFactory
import org.orekit.frames.TopocentricFrame
import org.orekit.utils.Constants
import org.orekit.utils.IERSConventions
class UserLocationManager {
    // Function to create the user's location as a TopocentricFrame
    fun createUserLocation(): TopocentricFrame {
        // Step 1: Hardcoded user's latitude, longitude, and altitude
        val userLatitude = 30.62 // Latitude in degrees (example value)
        val userLongitude = -96.34 // Longitude in degrees (example value)
        val userAltitude = 102.0 // Altitude in meters (example value)

        // Step 2: Convert latitude and longitude from degrees to radians
        val latitudeRadians = Math.toRadians(userLatitude)
        val longitudeRadians = Math.toRadians(userLongitude)

        // Step 3: Create a GeodeticPoint representing the user's position
        val geodeticPoint = GeodeticPoint(latitudeRadians, longitudeRadians, userAltitude)

        // Step 4: Create a BodyShape for the Earth (using WGS84 ellipsoid model)
        val earthShape = OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, true)
        )

        // Step 5: Create a Topocentric Frame for the user's location
        return TopocentricFrame(earthShape, geodeticPoint, "User Location")
    }

}