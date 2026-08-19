# academico-laboratorios-service

Microservicio de **Sistema de Control de Laboratorios Informáticos (SCLI)**.
Unifica temporalmente dos dominios relacionados:

- **Académico**: facultades, carreras, materias, periodos lectivos, horarios académicos.
- **Infraestructura**: campus, bloques, pisos, laboratorios, equipos informáticos.

Este servicio **no administra** contraseñas, JWT, roles, permisos, datos personales completos,
solicitudes, reservas ni aprobaciones — eso corresponde a otros microservicios del sistema
(`auth-service`, `usuarios-service`, `reservas-solicitudes-service`).

---

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje |
| Spring Boot 3.5.3 | Framework |
| Spring Web | API REST (Nivel 2 de Richardson) |
| Spring Data JPA + Hibernate | Persistencia |
| Spring Validation | Validación de DTOs |
| Spring Boot Actuator | Healthcheck (`/actuator/health`) |
| Flyway | Migraciones de base de datos |
| CockroachDB (driver PostgreSQL) | Base de datos (Database per Service) |
| Docker / Docker Compose | Contenerización |
| Maven | Build |

---

## Arquitectura del proyecto

```
src/main/java/ec/edu/scli/academico/
├── AcademicoLaboratoriosServiceApplication.java
├── entity/            # 11 entidades JPA
├── enums/              # EstadoPeriodo, EstadoLaboratorio, EstadoEquipo, DiaSemana
├── repository/         # 11 interfaces Spring Data JPA
├── dto/                # DTOs de entrada/salida por entidad + dto/internal
├── specification/       # Filtros dinámicos (Specification API)
├── service/ + service/impl/   # Lógica de negocio
├── controller/          # 11 controladores REST + InternalController
└── exception/           # Excepciones personalizadas + manejo global de errores

src/main/resources/
├── application.yml
└── db/migration/
    └── V1__crear_esquema_academico_laboratorios.sql
```

---

## Cómo levantar el proyecto

### Opción A — Con Docker Compose (recomendado)

Desde la **raíz del repositorio** (no dentro de esta carpeta):

```bash
docker compose up -d --build cockroach-academico cockroach-academico-init academico-laboratorios-service
```

Esto levanta la base de datos CockroachDB propia de este servicio (puerto `26259`), corre las
migraciones de Flyway automáticamente, y arranca la aplicación en el puerto `8083`.

Ver logs en vivo:

```bash
docker compose logs -f academico-laboratorios-service
```

### Opción B — Local, sin Docker (requiere una instancia de CockroachDB corriendo)

```bash
cd academico-laboratorios-service
../auth-service/mvnw spring-boot:run
```

### Variables de entorno

| Variable | Por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8083` | Puerto del servicio |
| `DB_URL` | `jdbc:postgresql://localhost:26259/academico_db?sslmode=disable&options=-c%20allow_unsafe_internals%3Dtrue` | URL de conexión |
| `DB_USERNAME` | `root` | Usuario de la base de datos |
| `DB_PASSWORD` | (vacío) | Contraseña |
| `INTERNAL_API_KEY` | `clave-interna-desarrollo` | Clave para autenticar los endpoints `/api/v1/internal/**` |

> El parámetro `allow_unsafe_internals=true` en la URL es necesario porque versiones recientes
> de CockroachDB restringen por defecto el acceso a tablas internas que Flyway necesita
> consultar para detectar la versión de la base de datos.

---

## Pruebas

```bash
cd academico-laboratorios-service
../auth-service/mvnw test
```

- `FacultadServiceImplTest` y `LaboratorioServiceImplTest`: pruebas unitarias con **Mockito**,
  no requieren base de datos (mockean los repositorios).
- `AcademicoLaboratoriosServiceApplicationTests`: prueba de contexto completo de Spring Boot,
  requiere una base de datos disponible (correr con Docker Compose levantado).

---

## Endpoints principales

Todos bajo el prefijo `/api/v1`. Paginación disponible en los `GET` de listado vía
`?page=0&size=20&sort=nombre,asc`.

### Facultades
| Método | Ruta |
|---|---|
| POST | `/facultades` |
| GET | `/facultades?codigo=&nombre=&activo=` |
| GET | `/facultades/{id}` |
| PUT | `/facultades/{id}` |
| PATCH | `/facultades/{id}/estado` |

### Carreras
| Método | Ruta |
|---|---|
| POST | `/carreras` |
| GET | `/carreras?facultadId=&codigo=&nombre=&activo=` |
| GET | `/carreras/{id}` |
| GET | `/facultades/{facultadId}/carreras` |
| PUT | `/carreras/{id}` |
| DELETE | `/carreras/{id}` (borrado lógico) |

### Materias
| Método | Ruta |
|---|---|
| POST | `/materias` |
| GET | `/materias?carreraId=&codigo=&nombre=&activo=` |
| GET | `/materias/{id}` |
| GET | `/carreras/{carreraId}/materias` |
| PUT | `/materias/{id}` |
| DELETE | `/materias/{id}` |

### Periodos lectivos
| Método | Ruta |
|---|---|
| POST | `/periodos-lectivos` |
| GET | `/periodos-lectivos?codigo=` |
| GET | `/periodos-lectivos/actual` |
| GET | `/periodos-lectivos/{id}` |
| PUT | `/periodos-lectivos/{id}` |

### Horarios académicos
| Método | Ruta |
|---|---|
| POST | `/horarios` |
| GET | `/horarios` |
| GET | `/horarios/{id}` |
| GET | `/horarios/docente/{docenteId}` |
| GET | `/horarios/laboratorio/{laboratorioId}` |

### Campus / Bloques / Pisos
| Método | Ruta |
|---|---|
| POST / GET / PUT / DELETE | `/campus`, `/campus/{id}` |
| GET | `/campus/{campusId}/bloques` |
| POST / GET / PUT / DELETE | `/bloques`, `/bloques/{id}` |
| GET | `/bloques/{bloqueId}/pisos` |
| POST / GET / PUT / DELETE | `/pisos`, `/pisos/{id}` |

### Laboratorios
| Método | Ruta |
|---|---|
| POST | `/laboratorios` |
| GET | `/laboratorios?texto=&estado=&activo=` |
| GET | `/laboratorios/disponibles` |
| GET | `/laboratorios/{id}` |
| PUT | `/laboratorios/{id}` |
| PATCH | `/laboratorios/{id}/estado` |

### Equipos
| Método | Ruta |
|---|---|
| POST | `/equipos` |
| GET | `/equipos?laboratorioId=&estado=&activo=` |
| GET | `/equipos/{id}` |
| GET | `/laboratorios/{laboratorioId}/equipos` |
| PUT | `/equipos/{id}` |
| PATCH | `/equipos/{id}/estado` |

### Tipos de equipo
| Método | Ruta |
|---|---|
| POST / GET / PUT / DELETE | `/tipos-equipo`, `/tipos-equipo/{id}` |

### Endpoints internos (para Reservas y Solicitudes Service)

Requieren el header `X-Internal-Api-Key` con el valor de `INTERNAL_API_KEY`.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/internal/laboratorios/{id}/disponibilidad-base` | Estructura + estado general (NO disponibilidad por fecha/hora) |
| GET | `/internal/laboratorios/{id}/exists` | Existencia de laboratorio |
| GET | `/internal/materias/{id}/exists` | Existencia de materia |
| GET | `/internal/periodos-lectivos/{id}/exists` | Existencia de periodo lectivo |

---

## Ejemplos con curl

```bash
# Crear una facultad
curl -X POST http://localhost:8083/api/v1/facultades \
  -H "Content-Type: application/json" \
  -d '{"codigo":"FISEI","nombre":"Facultad de Ingenieria","descripcion":"Sistemas e Industrial"}'

# Listar facultades activas
curl "http://localhost:8083/api/v1/facultades?activo=true"

# Crear un laboratorio (requiere un pisoId existente)
curl -X POST http://localhost:8083/api/v1/laboratorios \
  -H "Content-Type: application/json" \
  -d '{"pisoId":"<uuid-de-piso>","codigo":"LAB-101","nombre":"Laboratorio de Redes","capacidad":30}'

# Endpoint interno (consumido por Reservas Service)
curl "http://localhost:8083/api/v1/internal/laboratorios/<uuid>/disponibilidad-base" \
  -H "X-Internal-Api-Key: clave-interna-desarrollo"

# Healthcheck
curl http://localhost:8083/actuator/health
```

### Ejemplo en PowerShell (Windows)

```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/v1/facultades" -Method Post `
  -ContentType "application/json" `
  -Body '{"codigo":"FISEI","nombre":"Facultad de Ingenieria"}'
```

---

## Colección Postman

Importa el archivo [`postman_collection.json`](./postman_collection.json) incluido en esta
carpeta — trae los endpoints principales organizados por carpeta, con variables
`{{baseUrl}}` (`http://localhost:8083`) e `{{internalApiKey}}` ya configuradas.

---

## Reglas de negocio implementadas

- **Borrado lógico**: ningún registro se elimina físicamente; `DELETE` y `PATCH /estado`
  marcan `activo = false`.
- **Códigos únicos**: `codigo` de facultad, carrera, materia, laboratorio; `codigo_inventario`
  y `numero_serie` de equipo (verificados a nivel de aplicación y reforzados con `UNIQUE`
  en la base de datos).
- **Integridad referencial dentro del microservicio**: no se puede crear una `Carrera` sin
  una `Facultad` válida, ni un `Laboratorio` sin un `Piso` válido, etc.
- **Sin acoplamiento entre microservicios**: `docente_id` en `HorarioAcademico` es un UUID
  externo (de `usuarios-service`), almacenado sin validación ni llave foránea.
- **Separación de responsabilidades**: este servicio nunca decide si un laboratorio está
  disponible para una reserva específica — solo entrega su estado estructural general.
