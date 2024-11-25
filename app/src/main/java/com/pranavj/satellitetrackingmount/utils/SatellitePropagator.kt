package com.pranavj.satellitetrackingmount.utils

import android.util.Log
import org.orekit.bodies.GeodeticPoint
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.frames.FramesFactory
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.time.AbsoluteDate
import org.orekit.time.TimeScalesFactory
import org.orekit.utils.Constants
import org.orekit.utils.IERSConventions
import org.orekit.utils.PVCoordinates
import java.time.Instant
import java.util.Date
import org.hipparchus.util.FastMath

class SatellitePropagator {

    companion object {
        fun getCurrentStartDate(): AbsoluteDate {
            // Get the current instant in UTC
            val currentInstant = Instant.now()
            // Convert the Instant to a Date
            val currentDate = Date.from(currentInstant)


            // Create an AbsoluteDate from the Date and UTC time scale
            return AbsoluteDate(currentDate, TimeScalesFactory.getUTC())
        }

        private val earthShape = OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, true)
        )


        // Function to generate the path of azimuth and elevation for a given TLE

        /**
         * Generates azimuth/elevation data for a satellite relative to the user's location.
         */
        fun generateAzimuthElevationPath(
            tleLine1: String,
            tleLine2: String,
            startDate: AbsoluteDate,
            durationSeconds: Double,
            stepSeconds: Double,
            userTopocentricFrame: TopocentricFrame
        ): List<Pair<Double, Double>> {
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)
            val path = mutableListOf<Pair<Double, Double>>()
            var currentDate = startDate

            while (currentDate.durationFrom(startDate) <= durationSeconds) {
                val pvCoordinates = propagator.propagate(currentDate).pvCoordinates
                val azimuth = userTopocentricFrame.getAzimuth(
                    pvCoordinates.position,
                    userTopocentricFrame.parentShape.bodyFrame,
                    currentDate
                )
                val elevation = userTopocentricFrame.getElevation(
                    pvCoordinates.position,
                    userTopocentricFrame.parentShape.bodyFrame,
                    currentDate
                )

                path.add(Pair(Math.toDegrees(azimuth), Math.toDegrees(elevation)))
                currentDate = currentDate.shiftedBy(stepSeconds)
            }

            return path
        }



        /**
         * Generates a latitude/longitude path for a satellite.
         */
        fun generateLatLonPath(
            tleLine1: String,
            tleLine2: String,
            startDate: AbsoluteDate,
            duration: Double,
            stepSize: Double
        ): List<GeodeticPoint> {
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)

            val geodeticPoints = mutableListOf<GeodeticPoint>()
            val endDate = startDate.shiftedBy(duration)
            var currentDate = startDate

            while (currentDate.compareTo(endDate) <= 0) {
                val pvCoordinates = propagator.getPVCoordinates(currentDate, earthShape.bodyFrame)
                val geodeticPoint = earthShape.transform(pvCoordinates.position, earthShape.bodyFrame, currentDate)
                geodeticPoints.add(geodeticPoint)
                currentDate = currentDate.shiftedBy(stepSize)
            }

            return geodeticPoints
        }
    }
}
