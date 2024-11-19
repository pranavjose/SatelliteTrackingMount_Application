package com.pranavj.satellitetrackingmount.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pranavj.satellitetrackingmount.model.Satellite
import com.pranavj.satellitetrackingmount.R

class SatelliteAdapter(private val satellites: List<Satellite>) :
    RecyclerView.Adapter<SatelliteAdapter.SatelliteViewHolder>() {

    class SatelliteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val satelliteName: TextView = itemView.findViewById(R.id.tvSatelliteName)
        val satelliteNorad: TextView = itemView.findViewById(R.id.tvSatelliteNorad)
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
    }

    override fun getItemCount() = satellites.size
}