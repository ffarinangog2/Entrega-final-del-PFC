
# reservas-solicitudes-service

Microservicio SCLI encargado de gestionar solicitudes de reserva, reservas confirmadas,
disponibilidad y bloqueos de agenda de los laboratorios.

## Requisitos

- Java 21.
- CockroachDB local o disponible mediante Docker Compose.
- Puerto SQL `26260` disponible para CockroachDB.
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
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173` |
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
la máquina anfitriona.

## Seguridad

- API stateless protegida con JWT Bearer para operaciones de escritura.
- Las consultas `GET`, preflight CORS y Actuator quedan disponibles sin token.
- El token debe estar firmado con `JWT_SECRET`, pertenecer a `JWT_ISSUER` e incluir
  `sub` y `perfilId` como UUID.
- Los clientes internos envían `X-Internal-Api-Key` usando `INTERNAL_API_KEY`.

## API y observabilidad

- OpenAPI JSON: `http://localhost:8084/v3/api-docs`
- Swagger UI: `http://localhost:8084/swagger-ui.html`
- Health: `http://localhost:8084/actuator/health`
- Info: `http://localhost:8084/actuator/info`

Recursos principales:

- `/api/v1/solicitudes`
- `/api/v1/reservas`
- `/api/v1/agenda`
- `/api/v1/disponibilidad`

## Preparación local

Los comandos de Docker Compose deben ejecutarse desde la raíz del repositorio.
Los comandos Maven deben ejecutarse desde `reservas-solicitudes-service`.

### 1. Levantar la base de datos

```powershell
docker compose up -d cockroach-reservas cockroach-reservas-init
```

La base utiliza esta configuración:

- Motor: CockroachDB
- Contenedor: `scli-cockroach-reservas`
- Host: `localhost`
- Puerto SQL: `26260`
- Puerto administrativo: `8091`
- Base de datos: `reservas_db`
- Usuario: `root`
- Contraseña: vacía en desarrollo local inseguro

### 2. Verificar el contenedor

```powershell
docker compose ps cockroach-reservas cockroach-reservas-init
```

Antes de iniciar el microservicio, el contenedor debe aparecer saludable (`healthy`).

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
docker compose stop cockroach-reservas
```

La eliminación del volumen global debe coordinarse con el equipo para no afectar el
entorno compartido de Docker Compose.
