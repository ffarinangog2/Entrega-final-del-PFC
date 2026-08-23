# ADR-005: Patrones de diseño GoF aplicados en SCLI

## Estado
Aceptado (documento compartido — se edita por turnos: Harold → Iván → Freddy → Isaías, con `git pull --rebase` antes de cada edición)

## Contexto
La Guía Integral de la Entrega 4 (§5.2) exige aplicar al menos 5 patrones GoF, documentados con nombre y propósito en un único ADR, incluyendo obligatoriamente Repository, Factory Method y Strategy. La Tabla 1 de la guía asigna adicionalmente a FUVV: Singleton, Observer, State, Strategy y Facade. Este documento reúne, por integrante y por microservicio, los patrones realmente implementados (no placebo) con su ubicación exacta en el código.

## Decisión
Cada integrante documenta en su propia sección los patrones GoF aplicados en el microservicio bajo su responsabilidad.

---

## Harold Vinueza — academico-laboratorios-service

### Repository
**Propósito:** desacoplar el dominio de la tecnología de persistencia (JPA/Hibernate).
**Dónde:** interfaces `*RepositoryPort` en `domain/port/` (una por cada una de las 11 entidades), implementadas por adaptadores en `infrastructure/persistence/adapter/`.
**Por qué es real, no placebo:** el dominio (`domain/model`) no importa nada de JPA; los tests unitarios de `application/service/impl` usan dobles de prueba de estos puertos, sin necesitar base de datos real.

### Singleton
**Propósito:** un único punto de registro de métricas HTTP para todo el ciclo de vida de la aplicación, evitando que múltiples instancias corrompan el conteo de `http_requests_total`.
**Dónde:** `HttpRequestsMetricsRegistry` en `infrastructure/observability/`, registrado como bean único de Spring (ciclo de vida singleton del contenedor IoC) e inyectado en `HttpMetricsFilter`.
**Por qué es real:** Spring garantiza una sola instancia compartida entre todas las peticiones concurrentes; con múltiples instancias, cada una llevaría un conteo parcial y la métrica de Prometheus quedaría inconsistente.

### Facade
**Propósito:** simplificar la coordinación de 5 servicios de aplicación (Laboratorio, Piso, Bloque, Campus, Equipo) que, por separado, exigirían al cliente encadenar manualmente 5 llamadas y resolver a mano los IDs de relación para armar la ficha completa de un laboratorio.
**Dónde:** interfaz `LaboratorioDetalleFacade` (puerto) en `application/facade/`, implementada por `LaboratorioDetalleFacadeImpl` en `application/facade/impl/`. Expuesta en `GET /api/v1/laboratorios/{id}/detalle-completo` (`LaboratorioController`).
**Por qué es real, no placebo:** orquesta la cadena `Laboratorio → Piso → Bloque → Campus` (cada llamada resuelve el ID de relación que necesita la siguiente) más el listado de equipos, agregando todo en un único DTO (`LaboratorioDetalleCompletoResponse`). Esta operación es la base que consumirán el panel de monitoreo web y el escaneo QR móvil, evitando que cada cliente reimplemente la coordinación.

---

## Iván Villamarín — auth-service / api-gateway
_Pendiente: Factory Method + Repository._

## Freddy Farinango — reservas-solicitudes-service
_Pendiente: State + Strategy + Repository._

## Isaías Urbina — usuarios-service

### Repository
**Propósito:** desacoplar el dominio de la tecnología de persistencia (JPA/Hibernate).
**Dónde:** interfaces `*RepositoryPort` en `domain/port/` (una por cada una de las 5 entidades: `AdministradorRepositoryPort`, `DocenteRepositoryPort`, `EstudianteRepositoryPort`, `PerfilRepositoryPort`, `TecnicoRepositoryPort`), implementadas por adaptadores `*RepositoryAdapter` en `infrastructure/persistence/` (`PerfilRepositoryAdapter`, etc., cada uno delegando en su repositorio Spring Data JPA de `infrastructure/persistence/jpa/`).
**Por qué es real, no placebo:** el dominio (`domain/model`) no conoce JPA; `PerfilServiceImpl` depende solo de los puertos, y los tests de `application/service/*Test.java` (por ejemplo `PerfilServiceObserverTest`) usan mocks de Mockito de estos puertos sin necesitar base de datos real.

### Observer
**Propósito:** notificar cambios de estado de un perfil (creación, activación/desactivación) a interesados desacoplados del caso de uso que los origina, sin que `PerfilServiceImpl` conozca quién consume el evento.
**Dónde:** el evento `PerfilEvent` (record con factorías `creado()` y `estadoCambiado()`) y la interfaz `PerfilEventListener` en `domain/event/`; el listener concreto `LoggingPerfilEventListener` en `infrastructure/observer/`. El sujeto es `PerfilServiceImpl` (`application/service/`), que recibe `List<PerfilEventListener>` inyectada por Spring y notifica a todos en `publicarEvento()`, invocado desde `crear()`, `actualizar()` y `eliminar()`.
**Por qué es real, no placebo:** Spring inyecta automáticamente todos los beans `PerfilEventListener` existentes en la lista (hoy solo `LoggingPerfilEventListener`, pero agregar un segundo observador —p. ej. auditoría o notificaciones— no requiere tocar `PerfilServiceImpl`); el test `PerfilServiceObserverTest` lo prueba registrando un listener de prueba (`eventos::add`) en lugar del real y verificando que `crear()` dispara `PERFIL_CREADO` sin necesitar logging real.

---

## Consecuencias

**Positivas:**
- Los 7 patrones exigidos entre Repository, Factory Method, Strategy, Singleton, Observer, State y Facade quedan documentados con ubicación exacta en el código, no como descripciones genéricas.
- Cada patrón resuelve un problema real del dominio, evitando el riesgo de "patrones placebo" señalado por la guía.

**Negativas:**
- Al ser un archivo compartido editado por turnos, requiere disciplina de `pull --rebase` antes de cada edición para evitar conflictos de merge.

**Neutrales:**
- No afecta a los artefactos de E1-E3.