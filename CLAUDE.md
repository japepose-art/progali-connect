# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About This Project

Android provisioning app for Progali devices — configures WiFi and server settings over BLE using the BLUFI protocol (ESP32).

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew :app:test --tests "com.progali.connect.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean build
./gradlew clean build
```

## Architecture

MVVM with Jetpack Compose and a unidirectional data flow:

```
UI Screens (Compose) → ViewModel → Repository → Data Layer (BLE/BLUFI)
```

**Two screens, single activity:**
- `ScanScreen` — discovers TSBLU* BLE devices, handles runtime permissions, navigates to device config on connection
- `DeviceScreen` — configures WiFi credentials and server domain/port, can trigger device reboot

**State management:** Kotlin `StateFlow` throughout. ViewModels expose flows; screens collect them with `collectAsState()`.

**Dependency injection:** Hilt. `NetworkModule` (despite the name) provides singleton instances of `BleScanner`, `BlufiManager`, and `DeviceProvisionRepository`.

### BLE Communication Flow

1. `BleScanner` scans for devices whose name starts with `"TSBLU"` using LOW_LATENCY mode
2. `BlufiManager` manages the GATT connection and BLUFI protocol lifecycle (security negotiation → custom data exchange)
3. Custom text commands over BLUFI: `GET_SERVER`, `GET_PORT`, `SERVER:domain:port`, `REBOOT`
4. `DeviceProvisionRepository` aggregates both and is the single data source for ViewModels

### Key Files

| File | Purpose |
|------|---------|
| `data/ble/BleScanner.kt` | BLE scanning, exposes `foundDevices`/`isScanning` StateFlows |
| `data/ble/BlufiManager.kt` | GATT + BLUFI state machine, exposes connection/device state flows |
| `data/repository/DeviceProvisionRepository.kt` | Delegates to scanner and manager; single entry point for UI layer |
| `di/NetworkModule.kt` | Hilt module providing all BLE/data singletons |

## Module Structure

- `:app` — main application module (`com.progali.connect`)
- `:lib-blufi` — external ESP BLUFI library, sourced from `../references/EspBlufiForAndroid/lib-blufi`

## Key SDK / Library Versions

- Kotlin 2.1.0, AGP 8.13.2, compileSdk 36, minSdk 26
- Compose BOM 2025.02.00, Material 3
- Hilt 2.51.1 (KSP-based)
- Accompanist Permissions 0.34.0

## Contexto de Negocio y Reglas V1

- Los dispositivos usan el protocolo **BLUFI/Espressif BLE provisioning** (lib-blufi de ESP-IDF).
- El nombre BLE del dispositivo sigue el patrón `TSBLU` + últimos caracteres del UID del dispositivo.
- Solo se soporta **Wi-Fi 2.4 GHz** — no intentar provisionar redes 5 GHz.
- Comandos personalizados BLE disponibles: `GET_SERVER`, `GET_PORT`, `SERVER:<dominio>:<puerto>`, `PORT:<puerto>`, `REBOOT`.
- Flujo obligatorio V1: **scan → connect → wifi → server → validate → reboot**.
- **No integrar Room, Retrofit ni ningún backend en V1** — toda la lógica es provisioning local vía BLE.
- Prioridad de V1: provisioning local BLE funcional y estable. Nada más.

## Permissions

BLE permissions are split by Android version and handled at runtime in `ScanScreen`:
- Android 12+ (`S+`): `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
- Android < 12: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`

The `BLUETOOTH_SCAN` permission uses `neverForLocation` flag in the manifest.
