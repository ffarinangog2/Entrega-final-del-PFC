# ADR-006: Elección del stack de la aplicación móvil

## Estado

Implementado.

## Contexto

La Guía Integral de la Entrega 4 pide para FUVV un cliente móvil para técnicos con reservas rápidas,
notificación de incidentes y escaneo QR de sala. Además exige autenticación contra el mismo backend,
almacenamiento seguro del JWT, listado/detalle, modo sin conexión, pull-to-refresh, pruebas de ViewModel y
al menos una prueba instrumentada (Guía E4, Tabla 1 y §5.4). Entre las opciones de implementación que la
guía deja abiertas está Android Studio con Kotlin + Jetpack Compose (§5.1 y §5.4); no es la única permitida,
pero sí una de las válidas, y sobre esa base se tomó la decisión de este ADR.

El backend ya está armado como microservicios Spring Boot detrás de un API Gateway, y la web quedó en
React 18 con TypeScript. El módulo móvil (`apps/mobile/`) lo comparten los cuatro integrantes del equipo:
cada uno tiene su propia feature (login/sesión, escaneo QR, reservas rápidas, incidentes/push/offline)
dentro del mismo proyecto, bajo MVVM con un repositorio por feature.

### Problema

Necesitábamos un único stack para toda la app que permitiera:

- MVVM con repositorio independiente por feature, para que los cuatro pudiéramos trabajar en paralelo sin
  pisarnos dentro del mismo módulo;
- persistencia local para el modo sin conexión (Room o SQLDelight);
- notificaciones push (FCM) y acceso a cámara para el QR;
- almacenamiento seguro de JWT y consumo del mismo API Gateway que ya usa la web;
- minSdk 26 / targetSdk 34, tal como pide la guía;
- una curva de aprendizaje manejable, porque nadie del equipo venía con experiencia previa fuerte en
  desarrollo móvil nativo.

## Alternativas consideradas

### Java + Android Views (XML)

Es el enfoque nativo más maduro y probado, pero para las pantallas que necesitábamos (listados,
formularios, estados de carga/error) implica bastante más código repetitivo que Compose, y hoy Google ya no
concentra ahí sus librerías ni ejemplos nuevos.

### Flutter (Dart)

Permite un solo código base para Android/iOS y tiene buen rendimiento de UI. El problema es que mete un
lenguaje (Dart) que no se usa en ningún otro lugar del proyecto —ni en los microservicios (Kotlin/Java) ni
en la web (TypeScript)—, así que no hay ningún conocimiento que se pueda reaprovechar entre equipos, y las
integraciones nativas de Android (Room, WorkManager) dependen de paquetes de terceros en vez de librerías
oficiales de Google.

### React Native (TypeScript)

En papel es tentador porque comparte lenguaje con la web (React + TypeScript). En la práctica, todo lo que
necesitábamos de acceso nativo —cámara para el QR, guardado seguro del JWT, Room— requiere puentes o
librerías de terceros con menos respaldo oficial que ir nativo directamente, y el rendimiento de listas y
animaciones más exigentes queda por debajo de una UI nativa compilada.

### Kotlin + Jetpack Compose

Es el enfoque nativo que Google recomienda de forma oficial desde 2019. Se integra directo con Room,
DataStore, WorkManager y Firebase Cloud Messaging sin capas de traducción de por medio, y permite armar el
MVVM con Compose + ViewModel + StateFlow de forma bastante idiomática, con herramientas de testing propias
(`compose-ui-test`, `androidx.test`).

## Decisión

Se adopta **Kotlin + Jetpack Compose**, con arquitectura MVVM y un repositorio por feature, minSdk 26 y
targetSdk 34. El `compileSdk` puede terminar siendo mayor según qué plataforma tenga instalada cada entorno
de build; eso no cambia el target real de la app, solo la herramienta de compilación.

## Justificación técnica

Más allá de la preferencia del equipo, la decisión se apoya en dos datos concretos y verificables:

Por un lado, la adopción real de Kotlin en el ecosistema Android. No es una apuesta a algo nuevo: Android
es "Kotlin-first" desde Google I/O 2019, más del 95% de las 1000 apps más populares de la Play Store ya
usan Kotlin, y más de la mitad de los desarrolladores profesionales de Android lo tienen como lenguaje
principal (contra un 30% que sigue con Java) ([Android Developers – Kotlin-first](https://developer.android.com/kotlin/first)).
Elegir esto reduce bastante el riesgo de terminar con un stack en decadencia a mitad de proyecto.

Por otro, hay datos concretos de productividad al migrar de XML a Compose: equipos que hicieron ese cambio
reportan hasta 45% menos líneas de código para pantallas equivalentes y hasta 30% más velocidad para
construir features nuevas ([Jetpack Compose vs. XML Layout: Performance Comparison](https://medium.com/@atulsmart1996/jetpack-compose-vs-xml-layout-performance-comparison-b26538a72f40)).
Con cuatro personas construyendo features en paralelo y una fecha de entrega fija, ese ahorro de código
repetitivo por pantalla pesa bastante.

Ninguno de los dos criterios sale de una opinión nuestra: los dos están fechados y se pueden verificar en
las fuentes citadas.

## Impacto sobre el reparto del equipo

Justo por el MVVM + repositorio por feature que permite Compose, cada integrante pudo trabajar en su propia
carpeta (`features/qr/`, `features/reservas/`, `features/incidentes/`, etc.) dentro del mismo
`apps/mobile/` sin ramas personales y sin pisar el código de los demás. Esto ya se puso a prueba en la
práctica: mientras se desarrollaba la feature de reservas, en paralelo se armaba incidentes/push/offline
sobre el mismo proyecto, y ambos conjuntos de cambios se integraron sin conflictos de lógica real —solo
algún ajuste menor en archivos compartidos como el contenedor de inyección de dependencias y la base Room.

## Impacto sobre el backend y la arquitectura existente

No cambia ningún contrato REST, DTO ni configuración del Gateway. La app consume los mismos endpoints que
la web, se autentica contra el mismo `auth-service` y usa el mismo esquema de JWT. No hace falta ningún
backend adicional pensado solo para móvil.

## Consecuencias

### Consecuencias positivas

- Mucha documentación oficial y de comunidad disponible, por ser la opción de primera clase de Google.
- Menos código repetitivo por pantalla, lo que ayuda a llegar con el plazo de la Entrega 4.
- Integración directa con Room, DataStore y Firebase Cloud Messaging, sin capas intermedias.
- Testing con herramientas oficiales (`compose-ui-test`, `androidx.test`), consistente con el resto del
  stack de pruebas del proyecto (JUnit, MockK).

### Consecuencias negativas

- Compose usa más memoria y tiene picos de CPU por recomposición frente a Views puras en pantallas
  simples, según las mismas comparaciones citadas arriba.
- Sigue evolucionando activamente, así que pueden aparecer cambios entre versiones que obliguen a ajustar
  código existente —de hecho pasó durante el desarrollo, con un cambio de forma de invocación en APIs de
  `compose-ui-test` entre versiones.
- Alguien sin experiencia previa en Compose tiene una curva de aprendizaje distinta a la de XML, aunque la
  documentación oficial ayuda bastante a acortarla.
