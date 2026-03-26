package com.progali.connect.ui.device

import androidx.lifecycle.ViewModel
import com.progali.connect.data.repository.DeviceProvisionRepository
import blufi.espressif.params.BlufiConfigureParams
import blufi.espressif.params.BlufiParameter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val repository: DeviceProvisionRepository
) : ViewModel() {
    
    val connectionState = repository.connectionState
    val deviceStatus = repository.deviceStatus
    
    // Observamos los datos del servidor del repositorio
    val serverDomain = repository.serverDomain
    val serverPort = repository.serverPort

    fun updateDeviceStatus() {
        repository.requestDeviceStatus()
    }

    fun configureWifi(ssid: String, pass: String) {
        val params = BlufiConfigureParams()
        params.opMode = BlufiParameter.OP_MODE_STA
        params.staSSIDBytes = ssid.toByteArray()
        params.staPassword = pass
        repository.configureWifi(params)
    }

    fun configureServer(server: String, port: String) {
        val command = "SERVER:$server:$port".toByteArray()
        repository.postCustomData(command)
    }

    fun rebootDevice() {
        val command = "REBOOT".toByteArray()
        repository.postCustomData(command)
    }

    fun disconnect() {
        repository.disconnect()
    }
}
