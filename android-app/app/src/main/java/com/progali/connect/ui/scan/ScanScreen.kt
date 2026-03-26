package com.progali.connect.ui.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.progali.connect.data.ble.BlufiConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    onDeviceConnected: () -> Unit
) {
    val foundDevices by viewModel.foundDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Observamos el estado de conexión para navegar si tiene éxito
    LaunchedEffect(connectionState) {
        if (connectionState is BlufiConnectionState.Connected) {
            onDeviceConnected()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Escaneo de Dispositivos") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    if (isScanning) viewModel.stopScanning() else viewModel.startScanning()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isScanning) "Detener Escaneo" else "Iniciar Escaneo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning && foundDevices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(foundDevices) { result ->
                    DeviceItem(
                        name = result.device.name ?: "Desconocido",
                        address = result.device.address,
                        onClick = { viewModel.connectToDevice(result.device) }
                    )
                }
            }
        }
    }

    // Overlay simple para mostrar el estado de conexión
    if (connectionState is BlufiConnectionState.Connecting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Conectando...") },
            text = { Text("Por favor, espera mientras nos vinculamos con el dispositivo.") },
            confirmButton = {}
        )
    } else if (connectionState is BlufiConnectionState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.disconnect() },
            title = { Text("Error de Conexión") },
            text = { Text((connectionState as BlufiConnectionState.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.disconnect() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun DeviceItem(name: String, address: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(text = address, style = MaterialTheme.typography.bodySmall)
        }
    }
}
