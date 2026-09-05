# Evidencia FCM extremo a extremo

**HEAD auditado:** `cd61b64325480cbe132af7e56328f7fa5d8b99ef`

**Estado:** IMPLEMENTADO PARCIALMENTE — VALIDACIÓN E2E NO EJECUTADA.

## Cadena implementada

1. Android recibe/renueva el token en `ScliFirebaseMessagingService`.
2. `DeviceTokenRegistrar` registra o desregistra el dispositivo autenticado mediante `/api/v1/notificaciones/dispositivos`.
3. Reservas persiste el token por perfil y plataforma.
4. `NotificacionService` busca dispositivos activos.
5. Con `FIREBASE_ENABLED=true`, `FirebaseNotificationAdapter` envía mediante Firebase Admin SDK.
6. Existen disparadores para cambios de incidentes, solicitudes y planificación.

## Evidencia que falta

No están disponibles en el repositorio —correctamente, por tratarse de configuración sensible— `google-services.json` ni credenciales de cuenta de servicio. Tampoco existe una evidencia trazable de un evento real recibido en un dispositivo con Google Play Services.

Para cerrar E2E se debe configurar el mismo proyecto Firebase en cliente y servidor mediante secretos del entorno, registrar un dispositivo de prueba, provocar uno de los eventos soportados y conservar: SHA, fecha/hora, entorno, tipo de evento y captura de recepción sin token, UUID, credencial ni dato personal. Hasta entonces no corresponde declarar FCM operativo E2E.
