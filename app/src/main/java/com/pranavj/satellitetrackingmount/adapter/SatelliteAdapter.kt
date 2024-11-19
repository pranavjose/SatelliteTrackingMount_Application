package com.pranavj.satellitetrackingmount.adapter


import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.R
import android.widget.Button

class SatelliteAdapter(
    private val satellites: List<Satellite>,
    private val onPropagateClick: (org.orekit.propagation.analytical.tle.TLE) -> Unit
) : RecyclerView.Adapter<SatelliteAdapter.SatelliteViewHolder>() {

    class SatelliteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val satelliteName: TextView = itemView.findViewById(R.id.tvSatelliteName)
        val satelliteNorad: TextView = itemView.findViewById(R.id.tvSatelliteNorad)
        val propagateButton: Button = itemView.findViewById(R.id.btnPropagatePath)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatelliteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_satellite, parent, false)
        return SatelliteViewHolder(view)
    }

    override fun onBindViewHolder(holder: SatelliteViewHolder, position: Int) {
        val satellite = satellites[position]
        holder.satelliteName.text = satellite.name
        holder.satelliteNorad.text = "NORAD ID: ${satellite.noradCatalogNumber}"
        holder.propagateButton.setOnClickListener {
            try {
                // Create an Orekit TLE object using the satellite's data
                val tle = org.orekit.propagation.analytical.tle.TLE(satellite.line1, satellite.line2)
                // Call the propagation function in the activity
                onPropagateClick(tle) // Assuming `onPropagateClick` lambda is passed from the activity
            } catch (e: Exception) {
                Log.e("SatelliteAdapter", "Error creating TLE object: ${e.message}")
            }
        }
    }

    override fun getItemCount() = satellites.size
}