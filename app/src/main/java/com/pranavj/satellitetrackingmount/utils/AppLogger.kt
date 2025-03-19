package com.pranavj.satellitetrackingmount.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
object AppLogger {
    //[OLD]
//    private val logs = mutableListOf<String>()

    //new
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow() //read-only flow for UI

    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    //new

    fun log(tag: String, message: String) {
        //val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val timestamp = dateFormatter.format(Date())
//        logs.add("[$timestamp] [$tag]: $message")
        val logEntry = "[$timestamp] [$tag]: $message"

        _logs.value = _logs.value + logEntry
    }

//    fun getLogs(): List<String> {
//        return logs.toList() // Return a copy to avoid external modification
//    }

    fun clearLogs() {
//        logs.clear()
        _logs.value = emptyList()
    }
}