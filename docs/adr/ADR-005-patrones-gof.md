# ADR-005: Patrones GoF y límites arquitectónicos de SCLI

## Estado

Aceptado.

## Contexto

La Entrega 4 exige arquitectura en capas, principios SOLID y al menos cinco
patrones GoF demostrables. Repository, Factory Method y Strategy son
obligatorios; para FUVV también se revisan Singleton, Observer, State y Facade.

La auditoría cubre `auth-service`, `api-gateway`, `usuarios-service`,
`academico-laboratorios-service` y `reservas-solicitudes-service`. Un nombre de
clase o una anotación no se considera evidencia suficiente: cada patrón incluido
participa en una ruta real y tiene colaboradores o pruebas que demuestran su uso.

## Decisión

Se reconocen siete patrones vigentes: Repository, Factory Method, Strategy,
State, Observer, Facade y Singleton. No se agregan wrappers ni clases sin
comportamiento para incrementar artificialmente el conteo.

Los servicios con negocio se orientan a `presentation`, `application`, `domain`
e `infrastructure`. `api-gateway` es una excepción deliberada: es infraestructura
de ruteo, seguridad y observabilidad, sin modelo ni reglas empresariales propias.
Sus paquetes `config`, `routes` y `logging` son adecuados; crear capas de dominio
vacías sería una refactorización ficticia.

## Evidencia concreta en código

### Repository

- **Problema:** las reglas y casos de uso no deben conocer Spring Data, JPA ni
  entidades persistentes.
- **Contexto:** los servicios almacenan agregados y sus pruebas necesitan
  sustituir la persistencia.
- **Decisión:** definir puertos internos e implementarlos con adaptadores JPA.
- **Implementación principal (Villamarín):**
  `services/auth-service/src/main/java/ec/edu/uteq/scli/auth_service/domain/repository/UsuarioRepository.java`
  es el puerto y
  `services/auth-service/src/main/java/ec/edu/uteq/scli/auth_service/infrastructure/persistence/UsuarioPersistenceAdapter.java`
  lo implementa delegando en `UsuarioAuthRepository`. El mismo esquema existe
  con `RefreshSessionRepository` y `RefreshSessionPersistenceAdapter`.
- **Otras implementaciones:** `domain/port/*RepositoryPort` y adaptadores de
  infraestructura en Usuarios y Académico; `domain/port/out/*RepositoryPort` y
  `infrastructure/persistence/adapter/` en Reservas.
- **Participantes:** puerto, adaptador, repositorio Spring Data y servicio.
- **Consecuencias:** habilita Dependency Inversion, dobles de prueba y aislamiento
  de JPA; exige mapeo. En Académico varios puertos todavía exponen `Page` y
  `Pageable` de Spring Data, por lo que allí el aislamiento es parcial.
- **Responsables/microservicios:** todo el equipo; evidencia principal en
  `auth-service`, Villamarín.

### Factory Method

- **Problema:** la carga de usuarios no debe fijar cómo se construye el principal
  concreto de Spring Security.
- **Contexto:** un `Usuario` de dominio debe transformarse en objeto de
  autenticación sin acoplar el consumidor a su constructor.
- **Decisión:** delegar la creación mediante `crear(Usuario)`.
- **Implementación:**
  `services/auth-service/src/main/java/ec/edu/uteq/scli/auth_service/infrastructure/security/UserDetailsFactory.java`
  declara el método fábrica y `DefaultUserDetailsFactory.java` crea
  `CustomUserDetails`. `CustomUserDetailsService` depende de la interfaz.
- **Participantes:** creador, creador concreto y producto.
- **Consecuencias:** la construcción es sustituible y está probada por
  `DefaultUserDetailsFactoryTest` y `CustomUserDetailsServiceTest`; añade una
  abstracción, justificada porque ya se sustituye en pruebas.
- **Responsable/microservicio:** Villamarín, `auth-service`.

### Strategy

- **Problema:** la evaluación de disponibilidad puede variar sin reescribir el
  caso de uso.
- **Contexto:** el caso de uso coordina datos, pero la decisión debe permanecer
  como política pura del dominio.
- **Decisión:** inyectar una estrategia de disponibilidad.
- **Implementación:**
  `services/reservas-solicitudes-service/src/main/java/ec/edu/scli/reservas/domain/strategy/disponibilidad/DisponibilidadStrategy.java`
  define el algoritmo; `DisponibilidadSinConflictosStrategy.java` valida fecha,
  laboratorio, cruces y bloqueos. `application/config/StrategyConfig.java` la
  selecciona y `DisponibilidadServiceImpl.java` la consume.
- **Participantes:** contexto, estrategia y estrategia concreta.
- **Consecuencias:** favorece Open/Closed y pruebas sin Spring ni base de datos
  (`DisponibilidadSinConflictosStrategyTest`); hoy hay una sola implementación,
  pero la política es un punto real de variación.
- **Responsable/microservicio:** Freddy Farinango, Reservas.

### State

- **Problema:** evitar transiciones de estado dispersas en condicionales.
- **Contexto:** cada estado de solicitud o reserva permite acciones distintas y
  algunas transiciones tienen reglas temporales.
- **Decisión:** representar el comportamiento mediante objetos estado.
- **Implementación:** `domain/state/solicitud/SolicitudReservaState.java`, sus
  siete estados y `SolicitudReservaStates`; además
  `domain/state/reserva/ReservaState.java`, sus cinco estados y `ReservaStates`.
  `SolicitudReservaServiceImpl` y `ReservaServiceImpl` delegan las transiciones.
- **Participantes:** contexto, interfaz State, estados concretos y selector.
- **Consecuencias:** centraliza transiciones y permite sustitución Liskov,
  verificada por `SolicitudReservaStateTest` y `ReservaStateTest`; aumenta el
  número de clases y el selector debe actualizarse con el enum.
- **Responsable/microservicio:** Freddy Farinango, Reservas.

### Observer

- **Problema:** reaccionar a cambios de perfiles sin acoplar el caso de uso a
  logging, auditoría o futuras notificaciones.
- **Contexto:** crear, activar o desactivar un perfil genera hechos consumibles
  por varios interesados.
- **Decisión:** publicar `PerfilEvent` a una colección de listeners.
- **Implementación:**
  `services/usuarios-service/src/main/java/ec/edu/scli/usuarios/domain/event/PerfilEvent.java`
  representa el evento y contiene `creado` y `estadoCambiado`;
  `PerfilEventListener.java` define el observador;
  `infrastructure/observer/LoggingPerfilEventListener.java` es el observador
  concreto. `application/service/PerfilServiceImpl.java` es el sujeto y notifica
  mediante `publicarEvento`.
- **Participantes:** sujeto, evento, observador y observador concreto.
- **Consecuencias:** nuevos observadores no modifican el caso de uso y
  `PerfilServiceObserverTest` prueba la notificación; al ser síncrona, un
  observador lento podría aumentar la latencia.
- **Responsable/microservicio:** Isaías Urbina, Usuarios.

### Facade

- **Problema:** obtener una ficha de laboratorio exige coordinar Laboratorio,
  Piso, Bloque, Campus y Equipo.
- **Contexto:** exponer esa coordinación a controladores o clientes duplicaría
  llamadas y resolución de relaciones.
- **Decisión:** ofrecer una operación única sobre los cinco subsistemas.
- **Implementación:**
  `services/academico-laboratorios-service/src/main/java/ec/edu/scli/academico/application/facade/LaboratorioDetalleFacade.java`
  y `application/facade/impl/LaboratorioDetalleFacadeImpl.java`. La implementación
  invoca cinco servicios y construye `LaboratorioDetalleCompletoResponse`;
  `LaboratorioController` la expone.
- **Participantes:** fachada, implementación, cinco servicios y controlador.
- **Consecuencias:** simplifica una coordinación real; la fachada depende hoy de
  DTO de presentación, deuda que limita la pureza de capas pero no invalida el
  patrón.
- **Responsable/microservicio:** Harold Vinueza, Académico.

`api-gateway` también es una fachada arquitectónica mediante
`routes/GatewayRoutes.java`, pero no se contabiliza como segunda implementación
GoF: su función principal es infraestructura de ruteo y seguridad, no
coordinación de casos de uso empresariales.

### Singleton

- **Problema:** múltiples puntos de acceso a `http_requests_total` pueden
  fragmentar o duplicar el registro de métricas.
- **Contexto:** todas las peticiones pasan por `HttpMetricsFilter`.
- **Decisión:** mantener una sola instancia del registro durante la aplicación.
- **Implementación:**
  `services/academico-laboratorios-service/src/main/java/ec/edu/scli/academico/infrastructure/observability/HttpRequestsMetricsRegistry.java`
  es un componente singleton de Spring, conserva la instancia en un campo
  `static volatile` y la expone con `getInstance`; `HttpMetricsFilter.java` la
  consume.
- **Participantes:** Singleton y filtro cliente.
- **Consecuencias:** un punto compartido y fallo explícito antes de inicializar;
  mezcla IoC con estado estático y dificulta sustitución en pruebas. Inyección de
  constructor sería más idiomática, pero dejaría de ser el Singleton explícito.
- **Responsable/microservicio:** Harold Vinueza, Académico.

## Consecuencias

### Positivas

- Hay siete patrones con participantes, consumidores y pruebas o rutas reales.
- Repository y Strategy invierten dependencias hacia abstracciones del dominio.
- Factory Method, State y Observer tienen pruebas específicas.
- El gateway conserva una estructura apropiada y evita capas vacías.

### Negativas y deuda reconocida

- `auth-service` tiene cuatro capas y dominio libre de Spring/JPA, pero algunos
  servicios de aplicación importan clientes, seguridad, métricas y persistencia
  concretos, además de DTO de presentación. Su Dependency Inversion entre
  aplicación e infraestructura es parcial.
- `academico-laboratorios-service` tiene cuatro capas, pero varios puertos de
  dominio exponen `Page` y `Pageable` y su Facade usa DTO de presentación.
- `usuarios-service` mantiene limpio el dominio, aunque algunos servicios de
  aplicación construyen DTO de presentación.
- `reservas-solicitudes-service` tiene capas y puertos canónicos, pero convive
  con paquetes heredados (`client`, `entity`, `repository`, `security`) fuera de
  `infrastructure`, que algunos casos de uso importan directamente.
- Resolver esas deudas exige migrar contratos internos de forma coordinada; no
  se hace en este ADR porque podría alterar autenticación, payloads o
  persistencia.

### Neutrales

- Esta decisión documenta el código vigente; no cambia endpoints, payloads,
  JWT, RBAC, persistencia ni comportamiento de ejecución.
