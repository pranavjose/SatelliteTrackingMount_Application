package com.pranavj.satellitetrackingmount.utils
import java.text.SimpleDateFormat
import java.util.*
object AppLogger {
    private val logs = mutableListOf<String>()

    fun log(tag: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add("[$timestamp] [$tag]: $message")
    }

    fun getLogs(): List<String> {
        return logs.toList() // Return a copy to avoid external modification
    }

    fun clearLogs() {
        logs.clear()
    }
}