package com.progali.connect.data.repository

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import com.progali.connect.data.ble.BleScanner
import com.progali.connect.data.ble.BlufiConnectionState
import com.progali.connect.data.ble.BlufiManager
import com.progali.connect.data.ble.DeviceInfo
import blufi.espressif.params.BlufiConfigureParams
import blufi.espressif.response.BlufiStatusResponse
import kotlinx.coroutines.flow.StateFlow

class DeviceProvisionRepository(
    private val bleScanner: BleScanner,
    private val blufiManager: BlufiManager
) {
    val foundDevices: StateFlow<List<ScanResult>> = bleScanner.foundDevices
    val isScanning: StateFlow<Boolean> = bleScanner.isScanning
    val connectionState: StateFlow<BlufiConnectionState> = blufiManager.connectionState
    val deviceStatus: StateFlow<BlufiStatusResponse?> = blufiManager.deviceStatus
    
    val serverDomain: StateFlow<String?> = blufiManager.serverDomain
    val serverPort: StateFlow<String?> = blufiManager.serverPort
    val deviceInfo: StateFlow<DeviceInfo?> = blufiManager.deviceInfo
    val lastRawResponse: StateFlow<String?> = blufiManager.lastRawResponse
    val wifiNetworks = blufiManager.wifiNetworks
    val isWifiScanning = blufiManager.isWifiScanning
    val wifiConfigureResult = blufiManager.wifiConfigureResult

    fun startScan() {
        bleScanner.startScan()
    }

    fun stopScan() {
        bleScanner.stopScan()
    }

    fun connectToDevice(device: BluetoothDevice) {
        bleScanner.stopScan()
        blufiManager.connect(device)
    }

    fun configureWifi(params: BlufiConfigureParams) {
        blufiManager.configureWifi(params)
    }

    fun postCustomData(data: ByteArray) {
        blufiManager.postCustomData(data)
    }

    fun requestDeviceStatus() {
        blufiManager.requestDeviceStatus()
    }

    fun requestDeviceInfo() {
        blufiManager.requestDeviceInfo()
    }

    fun requestWifiScan() {
        blufiManager.requestWifiScan()
    }

    fun disconnect() {
        blufiManager.requestCloseConnection()
        blufiManager.close()
    }
}
