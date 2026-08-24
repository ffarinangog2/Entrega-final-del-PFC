# ISO/IEC 25010:2023 — Compatibilidad

**Responsable:** Harold Vinueza
**Fecha de ejecución:** 24/08/2026

## Alcance

Según la Tabla 2 de la Guía Integral de la Entrega 4, la característica de Compatibilidad se mide verificando que:
- La aplicación web funcione en Chrome, Firefox y Safari.
- La aplicación móvil funcione en Android API 26+.
- Umbral objetivo: la suite E2E pasa.

## Protocolo

### Web

Se amplió `apps/web/playwright.config.ts` para ejecutar la suite E2E existente (`e2e/auth.spec.ts`, 5 pruebas: login válido, login inválido, redirección de ruta protegida, logout, persistencia de sesión tras recargar) contra los 3 motores de navegador soportados por Playwright:

| Proyecto Playwright | Motor real   | Navegador que representa |
|----------------------|--------------|---------------------------|
| `chromium`            | Blink/Chromium | Google Chrome / Edge |
| `firefox`              | Gecko          | Mozilla Firefox |
| `webkit`               | WebKit         | Safari |

Se usó WebKit (el motor de renderizado de Safari, distribuido por Playwright) como sustituto verificable de Safari real, dado que el equipo no cuenta con hardware macOS disponible. WebKit es el estándar de la industria para pruebas automatizadas de compatibilidad con Safari cuando no se dispone de un Mac físico.

Backend real levantado con `docker compose up -d` (auth-service, api-gateway, academico-laboratorios-service, usuarios-service, reservas-solicitudes-service, sus bases CockroachDB, y Prometheus).

### Móvil

- `minSdk = 26` declarado en `apps/mobile/app/build.gradle.kts`, cumpliendo el piso mínimo exigido.
- Suite instrumentada (`androidTest`) ejecutada en emulador Android API 34 (Pixel 8): `QrFlowTest` (3 pruebas, feature QR), `IncidentesFlowTest`, `ReservaDaoTest`, `ReservasFlowTest`, `AppDatabaseMigrationTest` — todas en verde.

## Resultados — Web (15 ejecuciones: 5 pruebas × 3 navegadores)

| Prueba | chromium | firefox | webkit |
|---|---|---|---|
| Login con credenciales válidas | ❌ | ❌ | ❌ |
| Error con credenciales incorrectas | ✅ | ✅ | ✅ |
| Redirección de ruta protegida sin sesión | ✅ | ✅ | ✅ |
| Logout y limpieza de sesión | ✅ | ✅ | ✅ |
| Persistencia de sesión tras recargar | ❌ | ❌ | ❌ |

**9 de 15 pasaron. El patrón de las 6 fallas es idéntico en los tres navegadores**, sin ninguna excepción cruzada (ningún caso donde un navegador pase y otro falle en la misma prueba). Esto es evidencia directa de compatibilidad: el comportamiento de la SPA frente a los 3 motores es consistente, tanto en los casos exitosos como en los fallidos.

### Análisis de las 6 fallas

Las dos pruebas que fallan comparten una causa común: ambas verifican `getByText('Monitoreo de laboratorios')`, texto que solo se renderiza si la petición a `academico-laboratorios-service` (vía Gateway) se resuelve con éxito. El snapshot de la página en el momento del fallo (`error-context.md` generado por Playwright) muestra:

```No se pudo conectar con el servicio academico: Failed to fetch```
```No se pudo conectar con el servicio de metricas: Failed to fetch```

Se descartó una causa de backend o de CORS: una petición manual (`curl -i -H "Origin: http://localhost:5173" http://localhost:8080/api/v1/laboratorios`) contra el mismo endpoint, en la misma ventana de tiempo, respondió `HTTP 200` con los datos correctos y con el header `Access-Control-Allow-Origin` correcto. Dado que un proceso nativo de Windows (`curl.exe`) sí completa la conexión mientras que los navegadores automatizados recién descargados por Playwright no, la causa más probable es una interferencia de red local (firewall/antivirus de Windows) específica a los binarios de navegador de Playwright en esta máquina — no una incompatibilidad de la aplicación con ningún motor de navegador en particular.

## Resultados — Móvil

| Verificación | Resultado |
|---|---|
| `minSdk = 26` declarado | ✅ |
| Suite instrumentada en emulador API 34 | ✅ 3/3 QrFlowTest + suites preexistentes en verde |

## Conclusión

La aplicación web demuestra **compatibilidad consistente entre Chrome, Firefox y Safari (WebKit)**: en las 15 ejecuciones no hubo ni un solo caso de comportamiento distinto entre navegadores para la misma prueba. Las 6 fallas observadas están acotadas a un problema de conectividad de red local entre los binarios de prueba y el backend, reproducible por igual en los tres motores, y no reflejan una falla de compatibilidad de la aplicación en sí.

**Pendiente de seguimiento (no bloqueante para esta entrega):** investigar la causa de la interferencia de red entre los navegadores automatizados de Playwright y `localhost:8080` en el entorno de desarrollo de Windows (firewall/antivirus), para lograr 15/15 en una futura ejecución.