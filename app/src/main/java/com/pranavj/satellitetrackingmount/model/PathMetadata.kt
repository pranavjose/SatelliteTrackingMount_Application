package com.pranavj.satellitetrackingmount.model

import com.mapbox.geojson.Point

data class PathMetadata(
    val pathPoints: List<Point>, // Points that define the satellite's path
    val color: Int,              // Color for the path
    val startMarker: Point,      // Start position of the path
    val stopMarker: Point        // Stop position of the path
)

