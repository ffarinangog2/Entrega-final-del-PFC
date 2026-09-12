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

## Notificaciones push (Firebase) — implementación parcial

El cliente recibe mensajes, presenta notificaciones y registra el token en el
backend cuando existe una sesión autenticada. Reservas persiste esos dispositivos
y dispone de un adaptador Firebase. La cadena completa **no funciona ni está
validada E2E hasta que se agregue configuración real de Firebase**, que no se
puede generar sin acceso al proyecto Firebase del equipo:

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
5. Configurar en Reservas `FIREBASE_ENABLED=true` y proporcionar
   `FIREBASE_CREDENTIALS_BASE64` mediante el gestor de secretos del entorno, sin
   versionar el JSON de cuenta de servicio.
6. Ejecutar un evento real soportado (actualización de incidente, solicitud o
   planificación) y conservar evidencia de recepción en un dispositivo con
   Play Services. La existencia de tests unitarios no reemplaza esta prueba E2E.

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

## APK release firmado

La compilación release local sin credenciales se ejecuta en Windows con:

```powershell
.\gradlew.bat clean assembleRelease
```

El resultado sin firma queda en:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

La firma de publicación se realiza en GitHub Actions únicamente para pushes a
`feature/entrega-4` y cuando estén configurados estos secrets del repositorio:

- `ANDROID_SIGNING_KEYSTORE_BASE64`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`

CI reconstruye temporalmente el keystore dentro de `$RUNNER_TEMP`, alinea el APK
con `zipalign`, firma con `apksigner` y verifica la firma y el certificado con
`apksigner verify --verbose --print-certs`. Después genera y comprueba
`SHA256SUMS.txt` y publica ambos archivos en el artifact
`scli-mobile-release-<SHA>`:

```text
scli-mobile-0.1.0-release.apk
SHA256SUMS.txt
```

Para instalar el APK descargado y comprobar su `applicationId`:

```bash
adb install -r scli-mobile-0.1.0-release.apk
adb shell pm list packages ec.edu.uteq.scli.mobile
```

El keystore privado nunca se sube a Git. Debe conservarse cifrado y respaldado
en una ubicación externa controlada por el equipo: perderlo impediría firmar
futuras actualizaciones con la misma identidad. Esta preparación no demuestra
todavía una publicación release; E7 se completa cuando exista una ejecución CI
firmada, verificada y descargable.
