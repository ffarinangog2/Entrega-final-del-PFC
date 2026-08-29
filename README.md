# PFC — Entrega 4

## Sistema de Control de Laboratorios e Infraestructura

SCLI gestiona autenticación, usuarios institucionales, laboratorios, solicitudes y
reservas. La Entrega 4 integra cinco servicios backend, clientes web y Android,
CockroachDB, análisis PySpark, observabilidad, pruebas automatizadas y entrega de
imágenes Docker por SHA.

## Arquitectura real

| Componente | Responsabilidad |
| --- | --- |
| `services/auth-service` | Autenticación, tokens access/refresh, RBAC y recuperación de contraseña. |
| `services/usuarios-service` | Perfiles, usuarios y datos institucionales sensibles. |
| `services/academico-laboratorios-service` | Catálogos académicos, infraestructura y laboratorios. |
| `services/reservas-solicitudes-service` | Solicitudes, reservas, agenda y disponibilidad. |
| `services/api-gateway` | Punto de entrada, enrutamiento y controles transversales. |
| `apps/web` | Cliente React/TypeScript servido por Nginx. |
| `apps/mobile` | Cliente Android Kotlin/Jetpack Compose. |
| CockroachDB + Flyway | Persistencia, migraciones y clúster E3 de Reservas. |
| `spark/` | Lectura JDBC y transformaciones analíticas con PySpark. |
| `ops/` | Prometheus, Grafana, OpenTelemetry Collector, Loki y cAdvisor. |

Los clientes acceden mediante el API Gateway; los microservicios se comunican por la
red interna de Docker. Véanse los [ADR](docs/adr/) y [diagramas C4](docs/diagrams/).

## Estructura

```text
.
├── apps/                 # Web y Android
├── services/             # Cinco servicios Spring Boot
├── db/                   # Esquema y semillas reproducibles
├── spark/                # Pipeline PySpark
├── tests/                # Contratos, carga, integración y E2E
├── ops/                  # Observabilidad
├── experimentos/         # Protocolos y resultados ISO 25010
├── docs/                 # ADR, C4 y deployment
├── scripts/              # CI, datos demo y deployment
├── release/              # APK y capturas de la entrega
├── .github/workflows/    # ci-cd.yml y cd.yml
├── docker-compose.yml
└── docker-compose.prod.yml
```

Playwright vive funcionalmente en `apps/web/e2e/`; `tests/e2e-web/` y
`tests/integration/` conservan la estructura de entrega, mientras el gate real se
orquesta desde CI.

## Requisitos

Para el stack completo bastan Docker Engine y Docker Compose v2. Para desarrollo sin
contenedores, el repositorio verifica:

| Herramienta | Versión/configuración |
| --- | --- |
| Java | 21 en backend y CI |
| Maven | Wrapper en cuatro servicios; Académico requiere Maven local |
| Node.js | 22.22.2 en tests/build y 20 en lint; se recomienda 22.22.2 |
| npm | Sin versión exacta fijada; usar `npm ci` |
| Gradle | Wrapper 8.13 |
| Android | compile/target SDK 34, minSdk 26 |
| Python | 3.12 en CI para Locust; PySpark requiere entorno compatible |

## Entorno y ejecución local

Copie el ejemplo y complete los valores requeridos. `.env` contiene valores locales
o reales y nunca debe versionarse:

```bash
git clone https://github.com/ffarinangog2/Entrega-final-del-PFC.git
cd Entrega-final-del-PFC
git switch feature/entrega-4
cp .env.example .env
# Complete .env sin registrar secretos en Git.
docker compose --env-file .env config --quiet
docker compose --env-file .env up -d --build
docker compose ps
```

En PowerShell, use `Copy-Item .env.example .env`. Emplee secretos aleatorios fuertes
para JWT, claves internas, cifrado/hash, SQL, SMTP y Grafana. `.env.example` cubre
las variables de Compose y analítica; sus campos obligatorios vacíos son
intencionales, por lo que Compose no validará hasta completarlos.

Accesos locales:

- Web: <http://localhost:3000>
- Gateway: <http://localhost:8080>
- Health: <http://localhost:8080/actuator/health>
- Grafana: <http://localhost:3001>
- Prometheus: <http://localhost:9090>

```bash
docker compose logs -f
docker compose down
```

`docker compose down` conserva volúmenes; no use `down -v` si necesita los datos.

## Puertos

| Componente | Puerto publicado → interno |
| --- | --- |
| Web / API Gateway | `3000 → 80` / `8080 → 8080` |
| Microservicios | No publicados; `8081`–`8084` internos |
| Cockroach Auth | `26257 → 26257`, `8088 → 8080` |
| Cockroach Usuarios | `26258 → 26257`, `8089 → 8080` |
| Cockroach Académico | `26259 → 26257`, `8090 → 8080` |
| Cockroach E3 (3 nodos) | `26261`–`26263 → 26257`; `8092`–`8094 → 8080` |
| cAdvisor / Prometheus / Grafana | `8085` / `9090` / `3001` |
| OpenTelemetry gRPC/HTTP | `4317` / `4318` |
| Loki | `3100` |

Estos puertos de datos y observabilidad son para diagnóstico local. En
`docker-compose.prod.yml` solo se publican Web y Gateway mediante `WEB_PORT` y
`API_GATEWAY_PORT`; el resto permanece interno.

## Web

La Web usa React 18, TypeScript, Vite, React Router, Vitest, Playwright e i18n
español/inglés. Incluye login, recuperación de contraseña, usuarios, reservas,
solicitudes, calendario, configuración y rutas protegidas por permisos.

```bash
cd apps/web
npm ci
npm run dev
npm run lint
npm test -- --run
npm run build
npm run test:e2e -- --project=chromium
```

Nginx sirve la imagen Docker y proxifica `/api/` y `/actuator/` hacia
`api-gateway:8080`, sin fijar la IP de la VM en el build. No se afirma un porcentaje
de cobertura: se validará en el bloque de calidad.

## Android

`apps/mobile` usa Kotlin, Compose y MVVM por feature. Incluye Retrofit, Room,
`EncryptedSharedPreferences`, DataStore, QR con CameraX/ML Kit, notificaciones
locales y receptor FCM.

```bash
cd apps/mobile
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

En Windows use `gradlew.bat`. CI ejecuta unitarias y lint y publica el APK debug
como artifact por SHA. Las pruebas `src/androidTest` existen, pero no son gate:
requieren emulador estable. Firebase permanece pendiente de `google-services.json`,
configuración FCM y emisor backend; no está completamente operativo.

## Pruebas

| Suite | Estado |
| --- | --- |
| Backend unitarias/integración | Implementado y automatizado con `mvn verify` en cinco servicios |
| Testcontainers/CockroachDB/Flyway | Implementado y automatizado; incluye gate real E3 |
| Web unitarias/build | Implementado y automatizado |
| Android unitarias/lint | Implementado y automatizado |
| Android instrumentadas | Implementado, no automatizado |
| Pact | Implementado; generación/verificación automatizada en matrices relacionadas |
| Integración Compose | Automatizado: health, login y petición autenticada |
| Playwright | Implementado y gate obligatorio |
| Locust | Gate controlado: 2 usuarios, 10 segundos, solo contra CI |
| Cobertura con umbral global | Pendiente |

Verificación manual del contrato de Reservas en Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\contract\verify-reservas-provider.ps1
```

## CI/CD

El workflow [`ci-cd.yml`](.github/workflows/ci-cd.yml) ejecuta:

```text
Push / Pull Request
 → lint → backend + Web + Android → CockroachDB E3
 → integración + Playwright + Locust
 → APK → seis imágenes GHCR por github.sha (solo push)
```

Publica `scli-auth-service`, `scli-usuarios-service`,
`scli-academico-laboratorios-service`, `scli-reservas-solicitudes-service`,
`scli-api-gateway` y `scli-web`. No usa `latest` para desplegar.

El workflow reusable [`cd.yml`](.github/workflows/cd.yml) se invoca únicamente así:

```text
push a main + CI/GHCR exitosos + CD_ENABLED=true
 → SSH → VM → docker-compose.prod.yml
 → scripts/deploy/deploy-vm.sh → healthcheck
```

Los PR y `feature/entrega-4` nunca despliegan; `CD_ENABLED=false` omite CD. Véanse
la configuración y el rollback por SHA en [deployment VM](docs/deployment-vm.md).

## Observabilidad e ISO 25010

Compose incluye Prometheus, Grafana, OpenTelemetry Collector, Loki y cAdvisor.
Grafana provisiona Prometheus y Loki, y hay dashboards separados para clúster/Raft,
Reservas, Académico y Usuarios en `ops/grafana/dashboards/`. El dashboard consolidado
`ops/grafana/pfc-dashboard.json` todavía falta y queda pendiente.

El [protocolo E4](experimentos/protocolo-e4.md), scripts y
[resultados](experimentos/resultados/) están en `experimentos/`. La presencia de
resultados no prueba su validación definitiva; este README no publica cifras ISO
25010 como concluyentes.

## Semillas reproducibles

`db/seeds.sql` y `db/seeds_01.sql`–`db/seeds_10.sql` generan datos deterministas
e idempotentes de Reservas. Los datos institucionales demo también son deterministas:

```powershell
$env:DEMO_DOCENTE_PASSWORD_HASH = '<HASH_BCRYPT_LOCAL>'
$env:DEMO_ADMIN_PISO_PASSWORD_HASH = '<HASH_BCRYPT_LOCAL>'
.\scripts\demo\seed-demo.ps1
```

Consulte [la guía demo](scripts/demo/README.md). Los hashes se suministran localmente.
Las semillas de volumen no se cargan automáticamente ni deben aplicarse en producción
sin aprobación.

## Deployment VM

[`docker-compose.prod.yml`](docker-compose.prod.yml) consume las seis imágenes GHCR
por `IMAGE_TAG`, sin reconstruir. [`deploy-vm.sh`](scripts/deploy/deploy-vm.sh)
hace pull, levanta el stack y exige Gateway `UP`. El administrador configura
`<SERVER_HOST>`, `<SERVER_USER>`, acceso GHCR de lectura y un `.env` fuera de Git.
El rollback usa un SHA anterior y preserva volúmenes CockroachDB.

## Documentación

- [ADR](docs/adr/)
- [Diagramas C4](docs/diagrams/)
- [Deployment VM](docs/deployment-vm.md)
- [Experimentos](experimentos/)
- [Observabilidad](ops/)
- [Pruebas](tests/)
- [Cliente móvil](apps/mobile/README.md)
- [Datos demo](scripts/demo/README.md)

## Pendientes conocidos

- Automatizar pruebas Android instrumentadas con un emulador estable.
- Completar Firebase/FCM y su emisor backend.
- Consolidar y validar `ops/grafana/pfc-dashboard.json`.
- Validar formalmente resultados y umbrales globales de cobertura/calidad.
- Unificar Node 20/22.22.2 en CI.
- Incorporar wrapper Maven al servicio académico o documentar Maven localmente.
