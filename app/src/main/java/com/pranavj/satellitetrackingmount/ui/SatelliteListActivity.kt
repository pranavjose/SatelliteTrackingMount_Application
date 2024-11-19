package com.pranavj.satellitetrackingmount.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pranavj.satellitetrackingmount.R
import com.pranavj.satellitetrackingmount.adapter.SatelliteAdapter
import com.pranavj.satellitetrackingmount.repository.SatelliteRepository
import kotlinx.coroutines.launch


class SatelliteListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_satellite_list)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewSatellites)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val repository = SatelliteRepository(applicationContext)

        lifecycleScope.launch {
            val satellites = repository.getAllSatellites() // Fetching data
            Log.d("SatelliteListActivity", "Number of satellites fetched: ${satellites.size}")
            val adapter = SatelliteAdapter(satellites)
            recyclerView.adapter = adapter
        }
    }
}