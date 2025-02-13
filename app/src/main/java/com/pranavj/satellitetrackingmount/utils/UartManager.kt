package com.pranavj.satellitetrackingmount.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.pranavj.satellitetrackingmount.utils.AppLogger
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.io.IOException



class UartManager(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var job: Job? = null
    private val ACTION_USB_PERMISSION = "com.pranavj.satellitetrackingmount.USB_PERMISSION"

    fun requestUsbPermission() {
        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()){
            AppLogger.log("UART", "No USB devices found.")
            return
        }

        val device = deviceList.values.first() //gets first detected USB device
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )

        if (!usbManager.hasPermission(device)){
            usbManager.requestPermission(device, permissionIntent)
            AppLogger.log("UART", "Requesting USB permission for device: ${device.deviceId}")
        } else {
            AppLogger.log("UART", "USB Permission already granted")
        }
    }

    fun isUsbPermissionGranted(): Boolean {
        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) return false

        val device = deviceList.values.first()
        return usbManager.hasPermission(device)
    }

    fun openSerialConnection(): Boolean {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()){
            AppLogger.log("UART", "No USB serial devices found.")
            return false
        }

        val driver: UsbSerialDriver = availableDrivers[0]
        val device = driver.device
        val connection = usbManager.openDevice(device) ?: return false

        if (connection == null) {
            AppLogger.log("UART", "Failed to open USB connection.")
            return false
        }

        if (driver.ports.isNotEmpty()) {
            serialPort = driver.ports[0]
            try {
                serialPort?.apply {
                    open(connection)
                    setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                    AppLogger.log("UART", "Serial connection opened at 9600 baud.")
                }
            } catch (e: IOException) {
                AppLogger.log("UART", "Error opening serial connection: ${e.message}")
                return false
            }
        } else {
            AppLogger.log("UART", "No available ports on USB device")
            return false
        }
        return true
    }

    fun sendData(data: String){
        serialPort?.let {
            try {
                it.write(data.toByteArray(), 1000)
                AppLogger.log("UART", "Send: $data")
            }
            catch (e: IOException) {
                AppLogger.log("UART", "Error sending data: ${e.message}")
            }
        } ?: AppLogger.log("UART", "Serial port not initialized.")
    }

    fun startStreaming(azElFlow: kotlinx.coroutines.flow.Flow<Pair<Double,Double>>){
        job = CoroutineScope(Dispatchers.IO).launch {
            azElFlow.collect() { (azimuth, elevation) ->
                val formattedData = "<AZ:$azimuth,EL:$elevation>\n"
                sendData(formattedData)
                delay(500) //sends every 0.5 sec
            }
        }
    }

    fun stopStreaming() {
        job?.cancel()
        serialPort?.close()
        serialPort = null
        AppLogger.log("UART", "Stopped UART Streaming")
    }
}