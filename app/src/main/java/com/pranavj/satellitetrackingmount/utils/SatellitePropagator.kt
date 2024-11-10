package com.pranavj.satellitetrackingmount.utils

import android.util.Log
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.time.AbsoluteDate
import org.orekit.time.TimeScalesFactory
import org.orekit.utils.PVCoordinates
import java.util.Date

class SatellitePropagator {

    // Function to generate the path of azimuth and elevation for a given TLE
    fun generateAzimuthElevationPath(
        tle: TLE,
        userTopocentricFrame: TopocentricFrame,
        startDate: AbsoluteDate,
        durationSeconds: Double,
        stepSeconds: Double
    ): List<Pair<Double, Double>> {
        val propagator = TLEPropagator.selectExtrapolator(tle)
        val path = mutableListOf<Pair<Double, Double>>()
        var currentDate = startDate

        // Generate path over the specified duration with given step intervals
        while (currentDate.durationFrom(startDate) <= durationSeconds) {
            val pvCoordinates = propagator.propagate(currentDate).pvCoordinates
            val azimuth = userTopocentricFrame.getAzimuth(pvCoordinates.position, userTopocentricFrame.getParent(), currentDate)
            val elevation = userTopocentricFrame.getElevation(pvCoordinates.position, userTopocentricFrame.getParent(), currentDate)

            // Convert to degrees
            val azimuthDegrees = Math.toDegrees(azimuth)
            val elevationDegrees = Math.toDegrees(elevation)

            // Add azimuth and elevation to the path
            path.add(Pair(azimuthDegrees, elevationDegrees))

            // Increment the time step
            currentDate = currentDate.shiftedBy(stepSeconds)
        }

        return path
    }

}
