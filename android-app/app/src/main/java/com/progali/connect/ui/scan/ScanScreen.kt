package com.progali.connect.ui.scan

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.progali.connect.data.ble.BlufiConnectionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    onDeviceConnected: () -> Unit
) {
    val foundDevices by viewModel.foundDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Gestión de permisos según la versión de Android
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionState = rememberMultiplePermissionsState(permissions = bluetoothPermissions)

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
            if (permissionState.allPermissionsGranted) {
                Button(
                    onClick = {
                        if (isScanning) viewModel.stopScanning() else viewModel.startScanning()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isScanning) "Detener Escaneo" else "Iniciar Escaneo")
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Se requieren permisos de Bluetooth y Ubicación para buscar dispositivos.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                            Text("Conceder Permisos")
                        }
                    }
                }
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

    // Overlays de conexión...
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

@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(title = { Text("Escaneo de Dispositivos (Preview)") })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Iniciar Escaneo")
                }
                Spacer(modifier = Modifier.height(16.dp))
                DeviceItem(name = "TSBLU-1234", address = "00:11:22:33:44:55", onClick = {})
                Spacer(modifier = Modifier.height(8.dp))
                DeviceItem(name = "TSBLU-5678", address = "AA:BB:CC:DD:EE:FF", onClick = {})
            }
        }
    }
}
