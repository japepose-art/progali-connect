package com.progali.connect.ui.device

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progali.connect.data.repository.DeviceProvisionRepository
import blufi.espressif.params.BlufiConfigureParams
import blufi.espressif.params.BlufiParameter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val repository: DeviceProvisionRepository
) : ViewModel() {

    val connectionState = repository.connectionState
    val deviceStatus = repository.deviceStatus
    val serverDomain = repository.serverDomain
    val serverPort = repository.serverPort
    val deviceInfo = repository.deviceInfo
    val lastRawResponse = repository.lastRawResponse
    val wifiNetworks = repository.wifiNetworks
    val isWifiScanning = repository.isWifiScanning
    val wifiConfigureResult = repository.wifiConfigureResult

    private val _isApplyingAll = MutableStateFlow(false)
    val isApplyingAll: StateFlow<Boolean> = _isApplyingAll.asStateFlow()

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
        viewModelScope.launch {
            Log.i("DeviceViewModel", "Configurando servidor: dominio='$server' puerto='$port'")
            repository.postCustomData("1:$server".toByteArray())
            delay(500)
            repository.postCustomData("2:$port".toByteArray())
            delay(500)
            repository.postCustomData("3:0".toByteArray())
            Log.i("DeviceViewModel", "Secuencia de servidor completada")
        }
    }

    /**
     * Secuencia completa de provisioning (servidor primero, Wi-Fi al final):
     * 1. Envía configuración de servidor (1:, 2:, 3:)
     * 2. Envía Wi-Fi vía BLUFI configure() — el dispositivo se conecta automáticamente
     *    al Wi-Fi y desconecta el BT al completar, por eso va al final.
     */
    fun applyAllAndReboot(ssid: String, wifiPass: String, server: String, port: String) {
        viewModelScope.launch {
            _isApplyingAll.value = true
            try {
                // Paso 1: Configurar servidor primero (antes de que el Wi-Fi desconecte el BT)
                Log.i("DeviceViewModel", "Paso 1: Enviando servidor '$server:$port'")
                repository.postCustomData("1:$server".toByteArray())
                delay(600)
                repository.postCustomData("2:$port".toByteArray())
                delay(600)
                repository.postCustomData("3:0".toByteArray())
                delay(800)

                // Paso 2: Configurar Wi-Fi vía BLUFI — desencadena conexión automática y desconexión BT
                Log.i("DeviceViewModel", "Paso 2: Enviando Wi-Fi SSID='$ssid'")
                val params = BlufiConfigureParams()
                params.opMode = BlufiParameter.OP_MODE_STA
                params.staSSIDBytes = ssid.toByteArray()
                params.staPassword = wifiPass
                repository.configureWifi(params)
            } finally {
                _isApplyingAll.value = false
            }
        }
    }

    fun rebootDevice() {
        repository.postCustomData("8:".toByteArray())
    }

    fun requestDeviceInfo() {
        repository.requestDeviceInfo()
    }

    fun requestWifiScan() {
        repository.requestWifiScan()
    }

    fun sendRawCommand(command: String) {
        repository.postCustomData(command.toByteArray())
    }

    fun disconnect() {
        repository.disconnect()
    }
}
