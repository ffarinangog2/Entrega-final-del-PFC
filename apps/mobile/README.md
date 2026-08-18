# apps/mobile

Scaffold mínimo de la app Android (Kotlin + Jetpack Compose).

- `minSdk`: 26
- `targetSdk` / `compileSdk`: 34
- Sin features todavía; solo estructura de proyecto Gradle y un `MainActivity` vacío.

## Wrapper de Gradle

Este scaffold se creó sin `gradle-wrapper.jar` (no había un entorno con Gradle
disponible al generarlo). `gradlew` / `gradlew.bat` y
`gradle/wrapper/gradle-wrapper.properties` ya están, pero antes de compilar hay
que generar el jar una vez, desde esta carpeta:

```
gradle wrapper --gradle-version 8.7
```

o abriendo el proyecto en Android Studio, que lo completa automáticamente.
