package com.pranavj.satellitetrackingmount.utils

import android.content.Context
import android.util.Log
import org.orekit.data.DataContext
import org.orekit.data.DirectoryCrawler
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object OrekitInitializer {
    fun initializeOrekit(context: Context) {
        // Destination directory in the app's internal storage
        val orekitDataDir = File(context.filesDir, "orekit-data")
        if (!orekitDataDir.exists()) {
            orekitDataDir.mkdir()
        }

        // Copy assets to internal storage
        try {
            copyAssetFolder(context, "orekit-data", orekitDataDir)
        } catch (e: IOException) {
            Log.e("OrekitInitializer", "Error copying assets", e)
        }

        // Initialize Orekit
        val manager = DataContext.getDefault().dataProvidersManager
        val crawler = DirectoryCrawler(orekitDataDir)
        manager.addProvider(crawler)
        Log.d("OrekitInitializer", "Orekit has been successfully initialized with data from: ${orekitDataDir.path}")
    }

    @Throws(IOException::class)
    private fun copyAssetFolder(context: Context, assetFolderPath: String, destDir: File) {
        val assetManager = context.assets
        val assetList = assetManager.list(assetFolderPath) ?: return

        for (asset in assetList) {
            val assetSubPath = "$assetFolderPath/$asset"
            val destFile = File(destDir, asset)

            if (assetManager.list(assetSubPath)?.isNotEmpty() == true) {
                // If it's a directory, create it and copy contents recursively
                destFile.mkdirs()
                copyAssetFolder(context, assetSubPath, destFile)
            } else {
                // If it's a file, copy it
                assetManager.open(assetSubPath).use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }
}
