# Evidencias visuales pendientes de Entrega 4

No se incluyen imágenes sintéticas ni capturas de Entrega 3 como evidencia de Entrega 4. Las dos figuras siguientes deben obtenerse de entornos reales y revisarse para eliminar secretos y datos personales.

## `panel-monitoreo.png`

1. Desplegar o identificar en la VM el mismo SHA que se documentará.
2. Abrir el dashboard SCLI provisionado desde `ops/grafana/pfc-dashboard.json`.
3. Seleccionar un rango temporal en el que existan datos reales.
4. Capturar el nombre del dashboard, el rango temporal y paneles con series visibles de solicitudes, latencia, errores, CockroachDB y recursos.
5. No mostrar login, credenciales, tokens, variables de entorno ni consultas que contengan información sensible.
6. Guardar la captura sin editar los datos como `docs/entrega-4/figuras/panel-monitoreo.png`.
7. Registrar junto al uso de la figura: SHA desplegado, fecha/hora con zona y entorno VM. Contrastar el SHA con el despliegue de GitHub Actions.

## `qr-scan.png`

1. Instalar el APK generado para el SHA documentado o compilar desde ese mismo SHA mediante GitHub Actions.
2. Conectar la aplicación SCLI al Gateway real autorizado.
3. Abrir el flujo QR y realizar un escaneo válido de prueba.
4. Capturar la pantalla de escaneo o su resultado reconocible dentro de SCLI.
5. Ocultar o usar datos de prueba para evitar JWT, UUID, contraseñas, correos, nombres y cualquier dato personal.
6. Guardar la captura como `docs/entrega-4/figuras/qr-scan.png`.
7. Registrar SHA del APK, fecha, API/dispositivo y entorno del Gateway. El reporte instrumentado puede complementar, pero no reemplazar, la captura real.

Hasta completar estos pasos, ambas figuras se consideran **evidencia visual pendiente** y el documento no debe referenciarlas como si existieran.
