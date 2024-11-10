package com.pranavj.satellitetrackingmount.repository

import android.content.Context
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.model.SatelliteDao
import com.pranavj.satellitetrackingmount.model.SatelliteDatabase
import com.pranavj.satellitetrackingmount.model.SatelliteDao.TLELines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.orekit.propagation.analytical.tle.TLE
import android.util.Log



class SatelliteRepository(private val context: Context) {
    
    private val satelliteDao: SatelliteDao by lazy {
        SatelliteDatabase.getDatabase(context).satelliteDao()
    }

    // Function to read the TLE file and return parsed satellite data
    fun getSatellitesFromTLE(): List<Satellite> {
        val tleLines = readTLEFile(context)
        return parseTLEFile(tleLines)
    }

    suspend fun getTLEObjects(): List<TLE> = withContext(Dispatchers.IO) {
        val tleLinesList = satelliteDao.getAllTLELines() // Fetch only line1 and line2
        val tleObjects = mutableListOf<TLE>()
        for (tleLines in tleLinesList) {
            try {
                // Create TLE object using line1 and line2
                val tle = TLE(tleLines.line1, tleLines.line2)
                tleObjects.add(tle)
                Log.d("TLE Creation", "TLE created successfully: ${tle.line1} | ${tle.line2}")
            } catch (e: Exception) {
                Log.e("TLE Creation Error", "Error creating TLE object: ${e.message}")
            }
        }
        return@withContext tleObjects
    }

    // Function to insert satellite data asynchronously
    suspend fun insertSatellites(satellites: List<Satellite>) {
        withContext(Dispatchers.IO) {
            try {
                satelliteDao.deleteAllSatellites() // Clear old data before inserting new
                satellites.forEach { satellite ->
                    satelliteDao.insertSatellite(satellite)
                }
            } catch (e: Exception) {
                e.printStackTrace() // Log the exception for debugging
            }
        }
    }

    // Function to parse the TLE data into a list of Satellite objects
    private fun parseTLEFile(tleLines: List<String>): List<Satellite> {
        val satellites = mutableListOf<Satellite>()

        var i = 0
        while (i < tleLines.size) {
            if (tleLines[i].isEmpty()) {
                i++
                continue
            }

            val name = tleLines[i].trim()
            val line1 = tleLines[i + 1]
            val line2 = tleLines[i + 2]

            val internationalDesignator = line1.substring(9, 17).trim()
            val noradCatalogNumber = line1.substring(2, 7).toInt()
            val inclinationDegrees = line2.substring(8, 16).trim().toDouble()
            val periodMinutes = 1440.0 / line2.substring(52, 63).trim().toDouble()

            val apogeeHeightKm = calculateApogee(line2)
            val perigeeHeightKm = calculatePerigee(line2)
            val eccentricity = "0." + line2.substring(26, 33).trim()

            val satellite = Satellite(
                internationalDesignator = internationalDesignator,
                noradCatalogNumber = noradCatalogNumber,
                name = name,
                periodMinutes = periodMinutes,
                inclinationDegrees = inclinationDegrees,
                apogeeHeightKm = apogeeHeightKm,
                perigeeHeightKm = perigeeHeightKm,
                eccentricity = eccentricity.toDouble(),
                line1 = line1,
                line2 = line2
            )

            satellites.add(satellite)
            i += 3
        }

        return satellites
    }

    // Function to read the TLE file from assets
    private fun readTLEFile(context: Context): List<String> {
        val tleLines = mutableListOf<String>()
        context.assets.open("amateurRadio.tle").bufferedReader().useLines { lines ->
            lines.forEach { line ->
                tleLines.add(line.trim())
            }
        }
        return tleLines
    }

    // Function to calculate apogee height (in km) from Line 2 of the TLE
    private fun calculateApogee(line2: String): Double {
        val eccentricityString = line2.substring(26, 33).trim()
        val meanMotionString = line2.substring(52, 63).trim()

        // Convert eccentricity and mean motion
        val eccentricity = ("0." + eccentricityString).toDouble()  // Eccentricity is in format .0000
        val meanMotionRevsPerDay = meanMotionString.toDouble()  // Mean motion in revolutions per day

        // Mean motion in radians per second
        val meanMotionRadPerSec = meanMotionRevsPerDay * 2 * Math.PI / 86400.0

        // Calculate semi-major axis (in km)
        val semiMajorAxis = Math.cbrt(MU / (meanMotionRadPerSec * meanMotionRadPerSec))

        // Calculate apogee distance (from Earth's center)
        val apogeeDistance = semiMajorAxis * (1 + eccentricity)

        // Calculate apogee height above Earth's surface
        return apogeeDistance - EARTH_RADIUS_KM
    }

    // Function to calculate perigee height (in km) from Line 2 of the TLE
    private fun calculatePerigee(line2: String): Double {
        val eccentricityString = line2.substring(26, 33).trim()
        val meanMotionString = line2.substring(52, 63).trim()

        // Convert eccentricity and mean motion
        val eccentricity = ("0." + eccentricityString).toDouble()  // Eccentricity is in format .0000
        val meanMotionRevsPerDay = meanMotionString.toDouble()  // Mean motion in revolutions per day

        // Mean motion in radians per second
        val meanMotionRadPerSec = meanMotionRevsPerDay * 2 * Math.PI / 86400.0

        // Calculate semi-major axis (in km)
        val semiMajorAxis = Math.cbrt(MU / (meanMotionRadPerSec * meanMotionRadPerSec))

        // Calculate perigee distance (from Earth's center)
        val perigeeDistance = semiMajorAxis * (1 - eccentricity)

        // Calculate perigee height above Earth's surface
        return perigeeDistance - EARTH_RADIUS_KM
    }

    // Companion object to store constants
    companion object {
        const val EARTH_RADIUS_KM = 6378.137  // Earth's radius in kilometers
        const val MU = 398600.4418  // Standard gravitational parameter for Earth (km^3 / s^2)
    }

}