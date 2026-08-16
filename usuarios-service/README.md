# usuarios-service

Microservicio SCLI encargado de gestionar los perfiles institucionales del sistema:
estudiantes, docentes, técnicos y administradores, además de exponer la verificación
interna de perfiles utilizada por `auth-service`.

## Requisitos

- Java 21.
- CockroachDB local o disponible mediante Docker Compose.
- Puerto SQL `26258` disponible para CockroachDB.
- Puerto `8082` disponible para el microservicio.

## URL base

```text
http://localhost:8082
```

## Preparación local

Los comandos de Docker Compose deben ejecutarse desde la raíz del repositorio.
Los comandos Maven deben ejecutarse desde `usuarios-service`.

### 1. Levantar la base de datos

```powershell
docker compose up -d cockroach-usuarios cockroach-usuarios-init
```

La base utiliza esta configuración:

- Motor: CockroachDB
- Contenedor: `scli-cockroach-usuarios`
- Host: `localhost`
- Puerto SQL: `26258`
- Puerto administrativo: `8089`
- Base de datos: `usuarios_db`
- Usuario: `root`
- Contraseña: vacía en desarrollo local inseguro

### 2. Verificar el contenedor

```powershell
docker compose ps cockroach-usuarios cockroach-usuarios-init
```

Antes de iniciar el microservicio, el contenedor debe aparecer saludable (`healthy`).

### 3. Compilar

```powershell
.\mvnw.cmd clean compile -DskipTests
```

### 4. Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway ejecutará automáticamente las migraciones disponibles en
`src/main/resources/db/migration`.

### 5. Verificar la salud

Abrir:

```text
http://localhost:8082/actuator/health
```

## Documentación interactiva (Swagger)

```text
http://localhost:8082/swagger-ui.html
```

Ahí se puede explorar y probar todos los endpoints, incluido el grupo **"Perfiles
internos"** documentado para el consumo desde `auth-service`.

## Endpoints disponibles

| Recurso | Base path |
|---|---|
| Perfiles | `/api/v1/perfiles` |
| Estudiantes | `/api/v1/estudiantes` |
| Docentes | `/api/v1/docentes` |
| Técnicos | `/api/v1/tecnicos` |
| Administradores | `/api/v1/administradores` |
| Perfiles internos (uso de `auth-service`) | `/api/v1/internal/perfiles` |

### Comunicación interna

El endpoint `GET /api/v1/internal/perfiles/{perfilId}/exists` requiere el header
`X-Internal-Api-Key` para autorizar la solicitud entre microservicios. El valor por
defecto en desarrollo es `clave-interna-desarrollo` (configurable con la variable de
entorno `INTERNAL_API_KEY`).

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `SERVER_PORT` | Puerto del microservicio | `8082` |
| `DB_URL` | URL JDBC de conexión a CockroachDB | `jdbc:postgresql://localhost:26258/usuarios_db?sslmode=disable` |
| `DB_USERNAME` | Usuario de la base de datos | `root` |
| `DB_PASSWORD` | Contraseña de la base de datos | *(vacía)* |
| `INTERNAL_API_KEY` | Clave para autorizar llamadas entre microservicios | `clave-interna-desarrollo` |

## Dependencias para operaciones reales

Este microservicio es consultado por:

- `auth-service`, para verificar la existencia y el estado de un perfil durante el
  inicio de sesión.
- `reservas-solicitudes-service`, para validar perfiles al crear solicitudes.

## Detener la base de datos

Desde la raíz del repositorio, conservando el volumen y sus datos:

```powershell
docker compose stop cockroach-usuarios
```

La eliminación del volumen global debe coordinarse con el equipo para no afectar el
entorno compartido de Docker Compose.
