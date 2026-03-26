package com.progali.connect.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BleScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val _foundDevices = MutableStateFlow<List<ScanResult>>(kotlin.collections.emptyList())
    val foundDevices: StateFlow<List<ScanResult>> = _foundDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: result.scanRecord?.deviceName
            val macAddress = result.device.address
            
            Log.d("BleScanner", "Detectado: $deviceName ($macAddress)")

            // Aplicamos de nuevo el filtro para mostrar solo equipos Progali
            if (deviceName != null && deviceName.startsWith("TSBLU", ignoreCase = true)) {
                _foundDevices.update { currentList ->
                    if (currentList.none { it.device.address == result.device.address }) {
                        currentList + result
                    } else {
                        currentList
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleScanner", "Error en escaneo BLE: $errorCode")
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val currentScanner = scanner
        if (currentScanner == null) {
            Log.e("BleScanner", "No se pudo obtener el BluetoothLeScanner")
            return
        }
        if (_isScanning.value) return

        _foundDevices.value = kotlin.collections.emptyList()
        _isScanning.value = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        Log.d("BleScanner", "Iniciando escaneo filtrado (TSBLU)...")
        currentScanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val currentScanner = scanner
        if (currentScanner == null || !_isScanning.value) return

        Log.d("BleScanner", "Deteniendo escaneo.")
        currentScanner.stopScan(scanCallback)
        _isScanning.value = false
    }
}
