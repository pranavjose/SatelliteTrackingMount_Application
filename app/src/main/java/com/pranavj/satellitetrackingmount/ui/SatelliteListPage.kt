package com.pranavj.satellitetrackingmount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.viewmodel.MainViewModel

@Composable
fun SatelliteListPage(mainViewModel: MainViewModel, navController: NavHostController) {
    // Collect the list of satellites from the ViewModel
    val satellites by mainViewModel.satellites.collectAsState()

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
                Text("Plot Path for the next 90min")
            }
            Button(onClick = {
                // Generate Azimuth/Elevation
                mainViewModel.generateAzimuthElevation(satellite.noradCatalogNumber)
            }) {
                Text("Send Az/El Data")
            }
        }
    }
}
