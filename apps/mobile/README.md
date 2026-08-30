# apps/mobile

App Android (Kotlin + Jetpack Compose), arquitectura MVVM + repositorio por feature.

- `minSdk`: 26
- `targetSdk` / `compileSdk`: 34
- El Gateway se configura con `SCLI_API_BASE_URL`. Para el emulador Android,
  usar `http://10.0.2.2:8080/` (la barra final es obligatoria).

## Wrapper de Gradle

El repositorio incluye `gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.properties` y `gradle-wrapper.jar` (Gradle 8.13).
No es necesario disponer de una instalación global de Gradle.

## Estructura

```
common/
  di/            AppContainer: DI manual (sin Hilt) instanciado en ScliMobileApplication
  logging/       Timber + JsonTree (logs en JSON con trace_id de sesión)
   navigation/    NavHost + bottom navigation, protegido por autenticación

data/local/      AppDatabase (Room)

features/
   auth/          login contra el Gateway + sesión en EncryptedSharedPreferences
  incidentes/    domain (modelo + interfaz de repositorio), data (Room),
                 presentation (ViewModel + pantalla con listado y formulario)
  notifications/ FirebaseMessagingService, canal de notificaciones,
                 solicitud de permiso POST_NOTIFICATIONS
  profile/       perfil del técnico + settings, persistido con DataStore
```

`IncidenteRepository` es una interfaz; hoy la única implementación es
`IncidenteLocalRepository` (Room). El día que se agregue una fuente remota
(Retrofit) — o una estrategia combinada local+remota — se implementa detrás de
esa misma interfaz sin tocar el ViewModel ni la UI.

## Notificaciones push (Firebase) — configuración pendiente

El código de `features/notifications/` está listo pero **no va a funcionar
hasta que se agregue configuración real de Firebase**, que no se puede generar
sin acceso al proyecto de Firebase del equipo:

1. Crear (o usar) un proyecto en [Firebase Console](https://console.firebase.google.com/)
   y registrar una app Android con `applicationId` = `ec.edu.uteq.scli.mobile`.
2. Descargar el `google-services.json` que genera Firebase Console y colocarlo
   en `apps/mobile/app/google-services.json`. Por defecto queda en
   `.gitignore` (no se commitea solo); si el equipo decide versionarlo hay que
   sacarlo a propósito del `.gitignore` de esta carpeta.
3. Habilitar **Firebase Cloud Messaging API (V1)** en Google Cloud Console
   para ese mismo proyecto.
4. El plugin `com.google.gms.google-services` en `app/build.gradle.kts` se
   aplica automáticamente en cuanto detecta `google-services.json`; hasta
   entonces el build sigue funcionando sin Firebase.
5. Falta además el lado servidor/backend que dispare los push — no es parte
   de este scaffold.

## Settings del técnico

Se guardan con Jetpack DataStore (Preferences), no con `SharedPreferences`:
nombre del técnico y el toggle de notificaciones habilitadas.

## Tests

- Unitarios/Robolectric (`src/test` y `src/testDebug`): ViewModels, repositorios,
  autenticación/refresh, mapeos, notificaciones y UI Compose.
- Instrumentados (`src/androidTest`): flujos de reservas, incidentes y QR, Room en
  memoria y migración de la base local.

CI ejecuta `testDebugUnitTest`, el reporte JaCoCo, Android lint y
`connectedDebugAndroidTest` en un emulador API 29. El APK debug se publica como
artefacto únicamente después de superar esos gates.
