
# reservas-solicitudes-service

Microservicio SCLI encargado de gestionar solicitudes de reserva, reservas confirmadas,
disponibilidad y bloqueos de agenda de los laboratorios.

## Requisitos

- Java 21.
- CockroachDB local o disponible mediante Docker Compose.
- Puertos SQL `26261`, `26262` y `26263` disponibles para los nodos E3 de Docker Compose.
- Puerto `8084` disponible para el microservicio.

## URL base

```text
http://localhost:8084
```

## Variables de entorno

| Variable | Obligatoria | Valor local predeterminado |
|---|---:|---|
| `SERVER_PORT` | No | `8084` |
| `DB_URL` | No | Conexión local a `reservas_db` por el puerto `26260` |
| `DB_USERNAME` | No | `root` |
| `DB_PASSWORD` | No | Vacío |
| `JWT_ISSUER` | No | `scli-auth-service` |
| `JWT_SECRET` | Sí | Sin valor predeterminado; Base64 de al menos 32 bytes |
| `INTERNAL_API_KEY` | No | `clave-interna-desarrollo` |
| `USUARIOS_SERVICE_URL` | No | `http://localhost:8082` |
| `ACADEMICO_LABORATORIOS_SERVICE_URL` | No | `http://localhost:8083` |
| `HTTP_CONNECT_TIMEOUT_MS` | No | `2000` |
| `HTTP_READ_TIMEOUT_MS` | No | `3000` |
| `HTTP_MAX_READ_RETRIES` | No | `2` |
| `LOG_LEVEL_ROOT` | No | `INFO` |
| `LOG_LEVEL_APP` | No | `INFO` |
| `LOG_LEVEL_REST_CLIENT` | No | `WARN` |

En Docker, las URL de servicios deben usar los nombres DNS de la red de contenedores.
`localhost` solamente es apropiado cuando los servicios se ejecutan directamente en
la máquina anfitriona. El valor `26260` anterior es exclusivamente el predeterminado
para una instancia local iniciada fuera del Compose; el despliegue Docker usa los tres
nodos `crdb-e3-1`, `crdb-e3-2` y `crdb-e3-3` por su puerto interno `26257`.

## Seguridad

- API stateless protegida con JWT Bearer para operaciones de escritura.
- Las consultas `GET` y Actuator quedan disponibles sin token.
- El token debe estar firmado con `JWT_SECRET`, pertenecer a `JWT_ISSUER` e incluir
  `sub` y `perfilId` como UUID.
- Los clientes internos envían `X-Internal-Api-Key` usando `INTERNAL_API_KEY`.
- El CORS externo de la aplicación Web es administrado por el API Gateway.
  Reservas se consume desde la Web exclusivamente a través del Gateway.

## API y observabilidad

- OpenAPI JSON: `http://localhost:8084/v3/api-docs`
- Swagger UI: `http://localhost:8084/swagger-ui.html`
- Health: `http://localhost:8084/actuator/health`
- Info: `http://localhost:8084/actuator/info`
- Métricas Prometheus: `http://localhost:8084/actuator/prometheus`
- Entrada externa por API Gateway: `http://localhost:8080`

Recursos principales:

- `/api/v1/solicitudes`
- `/api/v1/reservas`
- `/api/v1/agenda`
- `/api/v1/disponibilidad`

## Preparación local

Los comandos de Docker Compose deben ejecutarse desde la raíz del repositorio.
Los comandos Maven deben ejecutarse desde `services/reservas-solicitudes-service`.

### 1. Levantar la base de datos

```powershell
docker compose up -d crdb-e3-1 crdb-e3-2 crdb-e3-3 crdb-e3-init
```

La base utiliza esta configuración:

- Motor: CockroachDB
- Contenedores: `scli-crdb-e3-1`, `scli-crdb-e3-2` y `scli-crdb-e3-3`
- Hosts internos: `crdb-e3-1`, `crdb-e3-2` y `crdb-e3-3`
- Puertos SQL externos predeterminados: `26261`, `26262` y `26263`
- Puertos administrativos predeterminados: `8092`, `8093` y `8094`
- Base de datos: `reservas_db`
- Usuario: `root`
- Contraseña: vacía en desarrollo local inseguro

### 2. Verificar el contenedor

```powershell
docker compose ps crdb-e3-1 crdb-e3-2 crdb-e3-3 crdb-e3-init
```

Antes de iniciar el microservicio, los nodos deben estar en ejecución y
`crdb-e3-init` debe haber finalizado correctamente.

### 3. Compilar

```powershell
.\mvnw.cmd clean compile -DskipTests
```

### Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas unitarias validan el contrato JWT. La prueba de integración usa
Testcontainers para iniciar CockroachDB y comprobar las migraciones Flyway. Si
Docker no está disponible, esta última se omite automáticamente.

### 4. Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway ejecutará automáticamente las migraciones disponibles en
`src/main/resources/db/migration`.

### 5. Verificar la salud

Abrir:

```text
http://localhost:8084/actuator/health
```

## Dependencias para operaciones reales

Para crear o actualizar solicitudes reales también deben estar disponibles:

- `usuarios-service` en `http://localhost:8082`
- `academico-laboratorios-service` en `http://localhost:8083`

El microservicio consulta esos servicios para validar perfiles, docentes, laboratorios,
materias y períodos lectivos.

## Contratos y cliente móvil

Desde la raíz, el contrato Pact Consumer + Provider se verifica con:

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\contract\verify-reservas-provider.ps1
```

El cliente Mobile configura la entrada del Gateway mediante `SCLI_API_BASE_URL`; para
el Gateway local debe apuntar a `http://localhost:8080/` (o al host equivalente que
pueda alcanzar el dispositivo/emulador).

## Persistencia, concurrencia y clientes internos

- Flyway es la única fuente del esquema; Hibernate usa `ddl-auto: validate`.
- CockroachDB trabaja con aislamiento serializable.
- Las entidades mutables usan `@Version` para bloqueo optimista.
- Las transiciones críticas cargan solicitudes y reservas con bloqueo pesimista.
- Las referencias a otros microservicios son UUID sin claves foráneas locales.
- Los clientes REST tienen tiempos máximos configurables y reintentan únicamente
  lecturas fallidas por conectividad o respuestas `5xx`.

## Detener la base de datos

Desde la raíz del repositorio, conservando el volumen y sus datos:

```powershell
docker compose stop crdb-e3-1 crdb-e3-2 crdb-e3-3
```

La eliminación del volumen global debe coordinarse con el equipo para no afectar el
entorno compartido de Docker Compose.

## Cierre técnico previo a pruebas finales

- La API acepta exclusivamente JWT Bearer con claim `type=access`; un refresh token
  no autentica peticiones de Reservas.
- Se aplican los permisos existentes de Auth: `SOLICITUD_*`, `RESERVA_*`,
  `AGENDA_GESTIONAR` y `LABORATORIO_LEER`. Las lecturas de solicitudes se limitan al
  propietario, excepto para identidades con permisos de gestión ya definidos.
- La creación y aprobación son idempotentes y persistentes. La creación vincula la
  clave con operación, actor, SHA-256 canónico del payload y solicitud resultante.
- Las transacciones serializables críticas tienen hasta tres intentos solo ante
  conflictos transitorios de locking, con backoff acotado.
- Flyway es la fuente de verdad. Compose E3 carga V1 desde la carpeta de migraciones
  y la aplicación aplica V2/V3; `db/schema.sql` queda como referencia histórica.
- `num_replicas=3` se conserva en la inicialización del cluster E3.
- `mvn verify` exige al menos 80 % de líneas y 48 % de ramas para este módulo,
  frente a una línea base medida de 82,50 % y 48,22 % respectivamente.
- `EXPIRADA` no se automatiza porque el contrato vigente no define una duración o
  fecha límite de vigencia; hacerlo requeriría una decisión de negocio compartida.
