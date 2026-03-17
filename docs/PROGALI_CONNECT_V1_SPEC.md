\# Progali Connect V1



\## Objetivo



Desarrollar una app Android propia de Progali, con identidad visual corporativa, que permita a un técnico o instalador:



1\. detectar dispositivos nuevos por BLE

2\. conectarse al equipo

3\. cambiar el servidor/dominio de destino

4\. configurar la red Wi-Fi

5\. validar que la configuración ha quedado aplicada

6\. reiniciar el dispositivo



\## Alcance V1



La V1 debe centrarse en provisioning y parametrización inicial.



\## Incluye



\- escaneo BLE

\- conexión al dispositivo

\- cambio de servidor/dominio

\- configuración Wi-Fi

\- validación de la configuración

\- reinicio del dispositivo

\- branding Progali



\## No incluye



\- backend propio

\- dashboard

\- gestión de eventos

\- APIs avanzadas

\- OTA

\- iOS



\## Base técnica



\- Android nativo

\- Kotlin

\- BLE provisioning compatible con BLUFI

\- referencia técnica local: EspBlufiForAndroid



\## Flujo funcional principal



BLE Scan → Connect → Server/Domain Config → Wi-Fi Config → Validation → Reboot

