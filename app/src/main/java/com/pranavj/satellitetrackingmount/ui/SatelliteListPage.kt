package com.pranavj.satellitetrackingmount.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.utils.AppLogger
import com.pranavj.satellitetrackingmount.viewmodel.MainViewModel

@Composable
fun SatelliteListPage(mainViewModel: MainViewModel, navController: NavHostController) {
    // Collect the list of satellites from the ViewModel
    //val satellites by mainViewModel.satellites.collectAsState()
    val satellites by mainViewModel.sortedSatellites.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Page Header with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }

            // 👇 Group these in a Row to stay together
            Row {
                Button(onClick = { mainViewModel.requestUsbAccess() }) {
                    Text("Request USB Access")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    val success = mainViewModel.openSerialPort()
                    if (success) {
                        AppLogger.log("UART", "Serial connection opened successfully.")
                        Toast.makeText(context, "Serial connection opened successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        AppLogger.log("UART", "Failed to open serial connection.")
                        Toast.makeText(context, "Failed to open serial connection.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Open Serial Connection")
                }
            }

            Text(
                text = "Satellite List",
                style = MaterialTheme.typography.h5
            )
        }

        // Satellite List
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(satellites) { satellite ->
                SatelliteListItem(satellite = satellite, mainViewModel = mainViewModel)
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun SatelliteListItem(satellite: Satellite, mainViewModel: MainViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Satellite Name
        Text(
            text = satellite.name,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.primary
        )

        // Buttons for actions
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                // Plot Satellite Path
                mainViewModel.plotSatellitePath(satellite.noradCatalogNumber)
            }) {
                Text("Plot Path (next 90 min)")
            }
            var isStreaming by remember { mutableStateOf(false) }

            Button(onClick = {
                // Generate Azimuth/Elevation
                isStreaming = !isStreaming
                if (isStreaming) {
                    mainViewModel.startAzElStreaming(satellite.noradCatalogNumber)
                }
                else {
                    mainViewModel.stopAzElStreaming()
                }
//                mainViewModel.generateAzimuthElevation(satellite.noradCatalogNumber)
            }) {
                Text(if (isStreaming) "Stop Az/El Stream" else "Send Az/El Data")
            }
        }
    }
}
