package com.progali.connect.ui.scan

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import androidx.lifecycle.ViewModel
import com.progali.connect.data.ble.BlufiConnectionState
import com.progali.connect.data.repository.DeviceProvisionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: DeviceProvisionRepository
) : ViewModel() {

    val foundDevices: StateFlow<List<ScanResult>> = repository.foundDevices
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val connectionState: StateFlow<BlufiConnectionState> = repository.connectionState

    fun startScanning() {
        repository.startScan()
    }

    fun stopScanning() {
        repository.stopScan()
    }

    fun connectToDevice(device: BluetoothDevice) {
        repository.connectToDevice(device)
    }

    fun disconnect() {
        repository.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopScan()
    }
}
