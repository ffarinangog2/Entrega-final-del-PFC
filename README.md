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

## Flujo operativo actual del equipo

El flujo oficial de trabajo y despliegue es:

```text
PC de desarrollo → código y Git
GitHub Actions → CI/CD y pruebas pesadas
VM → ejecución del sistema desplegado
```

Los contenedores, CockroachDB, Testcontainers, Playwright y las campañas de
carga se validan en GitHub Actions o en el entorno de despliegue autorizado. El
equipo no usa el levantamiento completo con Docker Compose en los PC como flujo
operativo cotidiano ni modifica archivos directamente en la VM.

## Alternativa técnica de ejecución local

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

En Windows use `gradlew.bat`. CI ejecuta unitarias, lint y las pruebas
`src/androidTest` en un emulador API 29; estas pruebas instrumentadas forman parte
del gate previo al APK. El APK debug se publica como artifact por SHA. Firebase
permanece pendiente de `google-services.json`,
configuración FCM y emisor backend; no está completamente operativo.

## Pruebas

| Suite | Estado |
| --- | --- |
| Backend unitarias/integración | Implementado y automatizado con `mvn verify` en cinco servicios |
| Testcontainers/CockroachDB/Flyway | Implementado y automatizado; incluye gate real E3 |
| Web unitarias/build | Implementado y automatizado |
| Android unitarias/lint | Implementado y automatizado |
| Android instrumentadas | Implementado y automatizado en emulador (API 29) |
| Pact | Implementado; generación/verificación automatizada en matrices relacionadas |
| Integración Compose | Automatizado: health, login y petición autenticada |
| Playwright | Implementado y gate obligatorio |
| Locust | Gate controlado: 2 usuarios, 10 segundos, solo contra CI |
| Cobertura con umbral global | Web y backend tienen gates; Android reporta 45,69 % de líneas, pero no aplica todavía un umbral global de 70 % |

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

Los PR nunca despliegan. `feature/entrega-4` despliega solo con
`CD_FEATURE_ENABLED=true`; `main`, solo con `CD_ENABLED=true`. Véanse
la configuración y el rollback por SHA en [deployment VM](docs/deployment-vm.md).

## Observabilidad e ISO 25010

Compose incluye Prometheus, Grafana, OpenTelemetry Collector, Loki y cAdvisor.
Grafana provisiona Prometheus, Loki y Tempo, y hay dashboards separados para
clúster/Raft, Reservas, Académico y Usuarios en `ops/grafana/dashboards/`. El dashboard
consolidado `ops/grafana/pfc-dashboard.json` contiene seis paneles; la latencia móvil
es un proxy de endpoints del Gateway y no una medición exclusiva del cliente Android.

El [protocolo E4](experimentos/protocolo-e4.md), scripts y
[resultados](experimentos/resultados/) están en `experimentos/`. El
[resumen verificable](experimentos/resultados/RESUMEN-ISO25010-E4.md) identifica
el SHA probado, la evidencia preservada y su manifiesto SHA-256. La eficiencia fue
medida; la fiabilidad formal no fue ejecutada (`0/10`) y no se afirma disponibilidad.

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

## Documentación académica

El documento oficial de la Entrega 4 es
[`docs/entrega-4/main.tex`](docs/entrega-4/main.tex). El árbol formado por
`docs/main.tex`, `docs/Referencias.bib`, `docs/secciones/` y
`docs/Informe_E3_SCLI_LATEX.pdf` se conserva como documentación histórica y
trazabilidad de la Entrega 3; no es el entregable principal de E4.

### Compilación reproducible del informe oficial

El informe utiliza `pdflatex` y BibTeX. Se requiere una distribución LaTeX que
incluya ambos comandos y los paquetes declarados por
`docs/entrega-4/main.tex` (`babel` con español, `geometry` e `hyperref`). Desde
la raíz del repositorio:

```bash
cd docs/entrega-4
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

La primera pasada genera los auxiliares, BibTeX procesa `referencias.bib`, y
las dos pasadas finales resuelven citas y referencias cruzadas. El resultado
esperado es `docs/entrega-4/main.pdf`.

### Declaración de uso de IA generativa

El equipo utilizó IA generativa como apoyo para revisar redacción, explorar
alternativas técnicas y asistir en tareas de implementación. Toda propuesta fue
revisada por integrantes del equipo y contrastada mediante inspección, pruebas
y evidencia del repositorio. La responsabilidad por el código, el informe, las
decisiones y sus limitaciones corresponde al equipo. Ningún contenido generado
se acepta automáticamente como resultado experimental: solo se reportan datos
producidos por ejecuciones trazables y artefactos verificables.

- [ADR](docs/adr/)
- [Diagramas C4](docs/diagrams/)
- [Deployment VM](docs/deployment-vm.md)
- [Experimentos](experimentos/)
- [Observabilidad](ops/)
- [Pruebas](tests/)
- [Cliente móvil](apps/mobile/README.md)
- [Datos demo](scripts/demo/README.md)

## Pendientes conocidos

- Completar Firebase/FCM y su emisor backend.
- Incorporar una métrica E2E móvil identificable, en lugar del proxy por URI del Gateway.
- Elevar y aplicar un umbral global de cobertura Android; el reporte actual alcanza 45,69 % de líneas.
- Conservar fuera de Git los HTML e historiales Locust completos que permanecen en la VM; los artefactos canónicos seleccionados y sus hashes sí están versionados.
- Unificar Node 20/22.22.2 en CI.
- Incorporar wrapper Maven al servicio académico o documentar Maven localmente.
