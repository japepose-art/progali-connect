package com.progali.connect.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.progali.connect.data.ble.BlufiConnectionState
import blufi.espressif.params.BlufiParameter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    
    // Obtenemos dominio y puerto del equipo
    val currentDomain by viewModel.serverDomain.collectAsState()
    val currentPort by viewModel.serverPort.collectAsState()

    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var serverDomain by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("") }

    LaunchedEffect(connectionState) {
        if (connectionState is BlufiConnectionState.Disconnected) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección: Información del Dispositivo
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Información del Dispositivo", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Estado BT: $connectionState")
                    
                    if (deviceStatus != null) {
                        val status = deviceStatus!!
                        val modeText = when(status.opMode) {
                            BlufiParameter.OP_MODE_STA -> "Station (Cliente)"
                            BlufiParameter.OP_MODE_SOFTAP -> "SoftAP (Punto de Acceso)"
                            BlufiParameter.OP_MODE_STASOFTAP -> "Station + SoftAP"
                            else -> "Desconocido (${status.opMode})"
                        }
                        
                        // LIMPIEZA DE ESTADO WI-FI
                        val wifiStatusText = if (status.staConnectionStatus == 0) "Conectado a Wi-Fi" else "Desconectado"

                        Text("Modo Op: $modeText")
                        Text("Estado Wi-Fi: $wifiStatusText")
                        
                        if (!status.staSSID.isNullOrBlank()) {
                            Text("SSID Actual: ${status.staSSID}")
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Servidor actual: ${currentDomain ?: "Consultando..."}")
                        Text("Puerto actual: ${currentPort ?: "Consultando..."}")

                    } else {
                        Text("Obteniendo estado del hardware...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Sección: Configuración Wi-Fi
            Text("Configuración Wi-Fi (2.4GHz)", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = wifiSsid,
                onValueChange = { wifiSsid = it },
                label = { Text("Nombre de Red (SSID)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = wifiPass,
                onValueChange = { wifiPass = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.configureWifi(wifiSsid, wifiPass) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aplicar Wi-Fi")
            }

            HorizontalDivider()

            // Sección: Configuración Servidor
            Text("Configuración de Servidor", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = serverDomain,
                onValueChange = { serverDomain = it },
                label = { Text("Dominio o IP") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = serverPort,
                onValueChange = { serverPort = it },
                label = { Text("Puerto") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.configureServer(serverDomain, serverPort) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Actualizar Servidor")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.rebootDevice() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reiniciar Dispositivo")
            }
        }
    }
}
