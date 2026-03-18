\# Progali Connect V1 - Architecture



\## 1. Purpose



This document defines the recommended technical architecture for Progali Connect V1.



The application is an Android native app for provisioning Progali devices using BLE and Wi-Fi configuration.



Main flow:



1\. BLE device scan

2\. Device connection

3\. Server/domain configuration

4\. Wi-Fi configuration

5\. Validation of applied configuration

6\. Device reboot



\---



\## 2. Technology Stack



\- Android native application

\- Kotlin

\- BLE communication

\- BLUFI-compatible provisioning flow

\- Local reference project: EspBlufiForAndroid



\---



\## 3. Architectural Style



Recommended architecture:



\- UI layer

\- Domain layer

\- Data layer



Pattern:

\- MVVM for presentation

\- Use Cases for business flow

\- Repositories for hardware/configuration access



\---



\## 4. Main Modules



\### 4.1 UI Layer



Responsible for screens, navigation, user input and state display.



Suggested screens:



\- HomeScreen

\- DeviceScanScreen

\- DeviceDetailScreen

\- ServerConfigScreen

\- WifiConfigScreen

\- ValidationScreen

\- RebootScreen

\- ResultScreen



\---



\### 4.2 Domain Layer



Responsible for business rules and application flow.



Suggested use cases:



\- ScanDevicesUseCase

\- ConnectDeviceUseCase

\- ConfigureServerUseCase

\- ConfigureWifiUseCase

\- ValidateConfigurationUseCase

\- RebootDeviceUseCase



\---



\### 4.3 Data Layer



Responsible for communication with BLE device and local persistence.



Suggested components:



\- BleScanner

\- BleConnectionManager

\- BlufiProvisioningManager

\- ServerCommandManager

\- DeviceConfigurationRepository

\- LocalLogRepository



\---



\## 5. Proposed Package Structure



com.progali.connect

│

├── ui

│ ├── home

│ ├── scan

│ ├── device

│ ├── server

│ ├── wifi

│ ├── validation

│ ├── reboot

│ └── result

│

├── domain

│ ├── model

│ ├── repository

│ └── usecase

│

├── data

│ ├── ble

│ ├── blufi

│ ├── server

│ ├── repository

│ └── local

│

└── common

├── utils

├── state

└── logging



\---



\## 6. Main Functional Flow



\### Step 1 – Scan BLE devices

The app scans for nearby BLE devices and lists available devices compatible with Progali provisioning.



\### Step 2 – Connect to device

The technician selects the target device and the app establishes a BLE connection.



\### Step 3 – Configure server/domain

The server domain or IP address used by the device is configured and sent to the device.



\### Step 4 – Configure Wi-Fi

The technician enters the Wi-Fi SSID and password.  

The credentials are sent to the device using the BLUFI provisioning flow.



\### Step 5 – Validate configuration

The app verifies that the device accepted the configuration and reports success or failure.



\### Step 6 – Reboot device

The reboot command is sent to the device and the final provisioning result is shown.



\---



\## 7. Technical Guidelines



\- Keep BLE logic isolated from UI

\- Reuse BLE / BLUFI reference logic where possible

\- Avoid mixing V2 features into V1

\- Keep server configuration logic separated from Wi-Fi provisioning

\- Add basic error handling and status reporting

\- Prepare local logs for future diagnosis

\- Keep the architecture modular for future extension



\---



\## 8. Out of Scope for V1



The following features are explicitly excluded from Version 1:



\- Backend integration

\- Remote dashboards

\- Event visualization

\- OTA updates

\- iOS version

\- Advanced device management



These features may be considered for future versions.



\---



\## 9. Next Development Step



After defining this architecture, the next step is to:



1\. Create the Android project skeleton.

2\. Define navigation between screens.

3\. Integrate BLE scanning functionality.

4\. Implement the provisioning flow using BLUFI reference logic.

