package com.progali.connect.data.repository

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import com.progali.connect.data.ble.BleScanner
import com.progali.connect.data.ble.BlufiConnectionState
import com.progali.connect.data.ble.BlufiManager
import kotlinx.coroutines.flow.StateFlow

class DeviceProvisionRepository(
    private val bleScanner: BleScanner,
    private val blufiManager: BlufiManager
) {
    // Exponemos los flujos de datos del Scanner y del Manager
    val foundDevices: StateFlow<List<ScanResult>> = bleScanner.foundDevices
    val isScanning: StateFlow<Boolean> = bleScanner.isScanning
    val connectionState: StateFlow<BlufiConnectionState> = blufiManager.connectionState

    fun startScan() {
        bleScanner.startScan()
    }

    fun stopScan() {
        bleScanner.stopScan()
    }

    fun connectToDevice(device: BluetoothDevice) {
        // Al conectar, detenemos el escaneo para liberar recursos
        bleScanner.stopScan()
        blufiManager.connect(device)
    }

    fun disconnect() {
        blufiManager.requestCloseConnection()
        blufiManager.close()
    }
}
