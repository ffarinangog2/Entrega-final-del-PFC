# Evidencia ISO/IEC 25010 — Seguridad

**Responsable:** Iván Villamarín

**Fecha de revisión:** 2026-08-24

**Rama revisada:** `feature/entrega-4`

## A. Objetivo

Evaluar la característica **Seguridad** de ISO/IEC 25010 mediante evidencia verificable de protección JWT, autenticación y autorización, manejo de credenciales, respuestas HTTP 401/403, CORS, cabeceras HTTP y controles relacionados con OWASP Top 10 2021. La revisión es estática y se complementa con las pruebas unitarias locales de Auth y API Gateway; no se ejecutan Docker ni pruebas E2E.

## B. Alcance

La revisión cubre el API Gateway como entrada principal, `auth-service`, `usuarios-service`, `academico-laboratorios-service`, `reservas-solicitudes-service`, y el uso de las APIs por Web y Mobile. Web y Mobile no aportan endpoints de servidor a la matriz: son clientes. Se revisaron sus mecanismos de consumo/autenticación sin modificar esos módulos.

Fuentes principales: `GatewayRoutes.java`, `CorsConfig.java`, los `SecurityConfig.java`, filtros y proveedores JWT, todos los controladores REST, `application.yml`, `.env.example`, `docker-compose.yml`, `.github/workflows/ci.yml`, POM y pruebas existentes.

## C. Matriz de endpoints JWT

La unidad de conteo es cada combinación **método HTTP + patrón de ruta** declarada por un controlador. Además se registran `health`, `info` y `prometheus` de cada componente. La matriz fila por fila, apta como checklist, está en [`matriz-seguridad-endpoints.csv`](matriz-seguridad-endpoints.csv). La tabla siguiente contiene todos los patrones revisados, agrupados solo cuando comparten servicio, clasificación y evidencia:

| Servicio | Método | Endpoint | Público/Protegido | JWT requerido | Rol si aplica | Evidencia |
|---|---|---|---|---|---|---|
| Auth | POST | `/api/v1/auth/login`; `/api/v1/auth/refresh` | Público | No | N/A | `AuthController`; `SecurityConfig permitAll`; exclusión en `JwtAuthenticationFilter` |
| Auth | GET | `/actuator/health`; `/actuator/info`; `/actuator/prometheus` | Público | No | N/A | `SecurityConfig permitAll`; exposición en `application.yml` |
| Usuarios | POST, GET, PUT | `/api/v1/tecnicos`; `/api/v1/tecnicos/{id}` | Protegido | Sí | Autenticado | `TecnicoController`; `SecurityConfig.anyRequest().authenticated()`; filtro JWT |
| Usuarios | POST, GET, PUT | `/api/v1/estudiantes`; `/api/v1/estudiantes/{id}` | Protegido | Sí | Autenticado | `EstudianteController`; `SecurityConfig`; filtro JWT |
| Usuarios | POST, GET, PUT | `/api/v1/docentes`; `/api/v1/docentes/{id}`; GET `/api/v1/docentes/perfil/{perfilId}` | Protegido | Sí | Autenticado | `DocenteController`; `SecurityConfig`; filtro JWT |
| Usuarios | POST, GET, PUT | `/api/v1/administradores`; `/api/v1/administradores/{id}` | Protegido | Sí | Autenticado | `AdministradorController`; `SecurityConfig`; filtro JWT |
| Usuarios | POST, GET, PUT, PATCH, DELETE | `/api/v1/perfiles`; `/api/v1/perfiles/{id}`; PATCH `/api/v1/perfiles/{id}/estado` | Protegido | Sí | `USUARIO_CREAR`, `USUARIO_LEER`, `USUARIO_EDITAR`, `USUARIO_DESACTIVAR` | `PerfilController`; reglas por método de `SecurityConfig` |
| Usuarios | GET | `/api/v1/internal/perfiles/{perfilId}`; `/api/v1/internal/perfiles/{perfilId}/exists` | Interno, no público funcional | No | `X-Internal-Api-Key` | `InternalPerfilController` valida la clave; JWT está en `permitAll` |
| Usuarios | GET | `/actuator/health`; `/actuator/info`; `/actuator/prometheus` | Público | No | N/A | `SecurityConfig permitAll`; `application.yml` |
| Académico | POST, GET, PUT, DELETE | `/api/v1/tipos-equipo`; `/api/v1/tipos-equipo/{id}` | Protegido en el perímetro Gateway | Sí | Autenticado, sin roles nuevos | `TipoEquipoController`; `API Gateway SecurityConfig`; sin JWT interno en Académico |
| Académico | POST, GET, PUT, DELETE | `/api/v1/pisos`; `/api/v1/pisos/{id}`; GET `/api/v1/bloques/{bloqueId}/pisos` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `PisoController`; sin `SecurityConfig`/filtro JWT |
| Académico | POST, GET, PUT | `/api/v1/periodos-lectivos`; `/api/v1/periodos-lectivos/{id}`; GET `/api/v1/periodos-lectivos/actual` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `PeriodoLectivoController`; sin seguridad JWT |
| Académico | POST, GET, PUT, DELETE | `/api/v1/materias`; `/api/v1/materias/{id}`; GET `/api/v1/carreras/{carreraId}/materias` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `MateriaController`; sin seguridad JWT |
| Académico | POST, GET, PUT, PATCH | `/api/v1/laboratorios`; `/api/v1/laboratorios/{id}`; GET `/disponibles`, `/metricas/ocupacion`, `/{id}/detalle-completo`; PATCH `/{id}/estado` bajo la base anterior | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `LaboratorioController`; sin seguridad JWT |
| Académico | GET | `/api/v1/internal/laboratorios/{id}/disponibilidad-base`; `/api/v1/internal/laboratorios/{id}/exists`; `/api/v1/internal/materias/{id}/exists`; `/api/v1/internal/periodos-lectivos/{id}/exists` | Interno | No | `X-Internal-Api-Key` | `InternalController` valida la clave; no están incluidos en la ruta académica del Gateway |
| Académico | POST, GET | `/api/v1/horarios`; `/api/v1/horarios/{id}`; GET `/api/v1/horarios/docente/{docenteId}`; `/api/v1/horarios/laboratorio/{laboratorioId}` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `HorarioAcademicoController`; sin seguridad JWT |
| Académico | POST, GET, PUT, PATCH | `/api/v1/facultades`; `/api/v1/facultades/{id}`; PATCH `/api/v1/facultades/{id}/estado` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `FacultadController`; sin seguridad JWT |
| Académico | POST, GET, PUT, PATCH | `/api/v1/equipos`; `/api/v1/equipos/{id}`; GET `/api/v1/laboratorios/{laboratorioId}/equipos`; PATCH `/api/v1/equipos/{id}/estado` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `EquipoController`; sin seguridad JWT |
| Académico | POST, GET, PUT, DELETE | `/api/v1/carreras`; `/api/v1/carreras/{id}`; GET `/api/v1/facultades/{facultadId}/carreras` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `CarreraController`; sin seguridad JWT |
| Académico | POST, GET, PUT, DELETE | `/api/v1/campus`; `/api/v1/campus/{id}` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `CampusController`; sin seguridad JWT |
| Académico | POST, GET, PUT, DELETE | `/api/v1/bloques`; `/api/v1/bloques/{id}`; GET `/api/v1/campus/{campusId}/bloques` | Protegido en el perímetro Gateway | Sí | Pendiente de definir | `BloqueController`; sin seguridad JWT |
| Académico | GET | `/actuator/health`; `/actuator/info`; `/actuator/prometheus` | Público | No | N/A | `application.yml`; sin capa Spring Security |
| Reservas | POST, GET, PUT | `/api/v1/solicitudes`; `/api/v1/solicitudes/{id}`; GET `/solicitante/{solicitanteId}`, `/estado/{estado}`, `/{id}/historial`; POST `/{id}/revision`, `/{id}/aprobar`, `/{id}/rechazar`, `/{id}/cancelar` bajo la base | Protegido | Sí | `SOLICITUD_CREAR`, `SOLICITUD_LEER`, `SOLICITUD_APROBAR`, `SOLICITUD_RECHAZAR`, `SOLICITUD_CANCELAR` | `SolicitudReservaController`; reglas de `SecurityConfig`; filtro JWT |
| Reservas | GET, POST | `/api/v1/reservas`; `/api/v1/reservas/{id}`; GET `/laboratorio/{laboratorioId}`, `/responsable/{responsableId}`, `/calendario`; POST `/{id}/cancelar`, `/{id}/iniciar`, `/{id}/finalizar`, `/{id}/no-asistida` bajo la base | Protegido | Sí | `RESERVA_LEER`, `RESERVA_CANCELAR`, `AGENDA_GESTIONAR` | `ReservaController`; `SecurityConfig`; filtro JWT |
| Reservas | GET | `/api/v1/disponibilidad/laboratorios/{laboratorioId}` | Protegido | Sí | `LABORATORIO_LEER` | `DisponibilidadController`; `SecurityConfig`; filtro JWT |
| Reservas | GET, POST, DELETE | `/api/v1/agenda`; GET `/api/v1/agenda/laboratorios/{laboratorioId}`; POST `/api/v1/agenda/bloqueos`; DELETE `/api/v1/agenda/bloqueos/{id}` | Protegido | Sí | `RESERVA_LEER` o `AGENDA_GESTIONAR`; bloqueos: `AGENDA_GESTIONAR` | `AgendaController`; `SecurityConfig`; filtro JWT |
| Reservas | GET | `/actuator/health`; `/actuator/info`; `/actuator/prometheus` | Público | No | N/A | `SecurityConfig permitAll`; `application.yml` |
| API Gateway | GET | `/actuator/health`; `/actuator/info`; `/actuator/prometheus` | Público | No | N/A | `SecurityConfig permitAll`; `application.yml` expone solo `health,info,prometheus` |

### Rutas del Gateway

`GatewayRoutes.java` enruta Auth, Usuarios, Académico y Reservas. `SecurityConfig` aplica seguridad transversal antes del proxy: solo permite sin JWT los `POST` exactos de login/refresh (incluidos sus dos alias necesarios), `OPTIONS /**` y los tres `GET` explícitos de Actuator. Cualquier otra ruta requiere un Bearer válido. El decodificador HMAC usa `JWT_SECRET`, exige issuer `scli-auth-service`, timestamps válidos y claim `type=access`; los refresh tokens no se aceptan como acceso. El header `Authorization` no se elimina y se reenvía al destino.

El perímetro externo oficial queda limitado a `localhost:8080`: `docker-compose.yml` conserva `8080:8080` para el Gateway y elimina las publicaciones host `8081:8081`, `8082:8082`, `8083:8083` y `8084:8084`. Healthchecks y Prometheus usan puertos internos y nombres de servicio dentro de `scli-network`, por lo que no dependen de esas publicaciones host.

### D. Resultado de cobertura JWT

- Operaciones de controlador revisadas: **119**.
- Endpoints de Actuator explícitamente revisados: **15**.
- Total de filas de evidencia: **134**.
- Operaciones que deben requerir JWT: **111**.
- Correctamente protegidas por JWT en el perímetro externo oficial: **111**.
- Sin protección JWT en ese perímetro: **0**.
- Endpoints públicos o con control alternativo y fuera del denominador: login, refresh, 15 endpoints Actuator y 6 endpoints internos con API key.

Fórmula:

```text
111 / 111 × 100 = 100 %
```

**Resultado del perímetro externo oficial vía Gateway: 100 %.** Este resultado depende conjuntamente de la validación transversal y de que los microservicios no se publiquen al host. No significa defensa en profundidad completa: Académico no valida JWT dentro del contenedor y un actor ya presente en `scli-network` podría intentar acceder directamente al servicio. Tampoco se inventaron roles para Académico; el Gateway exige autenticación válida, no autorización funcional por rol.

## Revisión OWASP Top 10

| OWASP | Riesgo | Control existente | Evidencia del proyecto | Estado |
|---|---|---|---|---|
| A01 Broken Access Control | Lectura o modificación sin autorización; escalamiento horizontal/vertical | JWT transversal en Gateway; autoridades por ruta en Usuarios y Reservas; API key en endpoints internos | `api-gateway/config/SecurityConfig.java`; SecurityConfig internos; Compose sin puertos 8081-8084 | **Parcial**: el perímetro exige JWT para 111/111 operaciones, pero Académico aún carece de roles y validación JWT interna |
| A02 Cryptographic Failures | Exposición de contraseñas, tokens o datos en tránsito/reposo | BCrypt con coste 12; secreto JWT por variable; almacenamiento móvil cifrado | `auth/.../SecurityConfig.passwordEncoder`; `app.jwt.secret: ${JWT_SECRET}`; `EncryptedAuthStorage.kt` | **Parcial**: no se evidencia imposición TLS extremo a extremo ni rotación de secretos; existen defaults de desarrollo para claves internas/DB |
| A03 Injection | SQL/JSON/entrada manipulada | Bean Validation y repositorios JPA parametrizados | 22 archivos usan `@Valid`/`@Validated`; 26 repositorios extienden JPA; no se halló concatenación SQL en la revisión | **Parcial**: control presente, pero no hay prueba/escaneo específico de inyección evidenciado para todas las entradas |
| A04 Insecure Design | Controles ausentes desde el diseño y confianza excesiva en la red | Gateway como única entrada host, JWT stateless y API key interna | `GatewayRoutes.java`; `api-gateway/config/SecurityConfig.java`; `docker-compose.yml` | **Parcial**: se cerró el bypass host, pero no se evidencia threat model, rate limiting ni aislamiento/zero-trust dentro de la red Docker |
| A05 Security Misconfiguration | Actuadores, CORS o defaults inseguros | CORS y Spring Security explícitos en Gateway; Actuator limitado | `CorsConfig.java`; `SecurityConfig.java`; `application.yml` | **Parcial**: Gateway expone solo health/info/prometheus, pero `show-details: always` y los orígenes CORS siguen orientados a desarrollo |
| A06 Vulnerable and Outdated Components | Dependencias conocidas como vulnerables | Versiones fijadas en POM y `mvn verify`/`npm ci` en CI | Spring Boot y JJWT versionados; `.github/workflows/ci.yml` ejecuta builds y tests | **No evidenciado**: no se encontró OWASP Dependency-Check, Dependabot, SCA ni auditoría de vulnerabilidades en el workflow revisado |
| A07 Identification and Authentication Failures | Login débil, token inválido aceptado, sesión persistente | BCrypt 12, JWT firmado/validado, access y refresh diferenciados, sesión stateless, 401 JSON | `JwtService`; `JwtAuthenticationFilter`; `JwtAuthenticationEntryPoint`; `SecurityConfig` | **Parcial**: no se evidencian rate limit, bloqueo por intentos, MFA ni pruebas HTTP completas de token inválido |
| A08 Software and Data Integrity Failures | Dependencias/build o datos sin verificar | Lockfile web, versiones Maven, Actions identificadas por versión mayor, CI con tests | `package-lock.json`; POM; `actions/checkout@v4`, `setup-java@v4`; `mvn verify` | **Parcial**: Actions no están fijadas por SHA y no se evidencia firma/SBOM/verificación de artefactos |
| A09 Security Logging and Monitoring Failures | Ataques no detectados o secretos en logs | Logging estructurado y telemetría configurados; no se encontraron llamadas de log que incluyan token/password/Authorization/secret | configuraciones `logging`, OTel/Prometheus; búsqueda estática en fuentes Java | **Parcial**: esta evidencia no evalúa observabilidad; no se evidencian eventos/auditoría de login fallido, denegaciones 403 ni alertas de seguridad |
| A10 Server-Side Request Forgery | URL controlada por usuario usada desde backend | Destinos entre servicios proceden de configuración y el Gateway usa URIs configuradas | variables `*_SERVICE_URL`; `GatewayRoutes.java`; clientes internos con base URL configurada | **Parcial**: no se evidencian allowlists/validación anti-SSRF ni pruebas dedicadas |

Resumen: **0 Cumple, 9 Parcial, 1 No evidenciado**. Se evita marcar “Cumple” porque ninguna categoría dispone de evidencia suficiente para afirmar cobertura integral.

## Validación de 401 y 403

Pruebas encontradas en Auth:

- `JwtAuthenticationEntryPointTest.respondeUnauthorizedConErrorJsonCoherente`: verifica estado 401, cuerpo JSON y mensaje del entry point.
- `JwtServiceTest`: verifica que un token inválido no sea válido como access ni refresh.
- `JwtAuthenticationFilterTest`: verifica ausencia de cabecera, Bearer válido, Bearer vacío, token inválido y exclusión de rutas públicas. La prueba de token inválido comprueba que no se crea autenticación, pero no atraviesa toda la cadena hasta una respuesta HTTP 401.
- `GlobalExceptionHandlerTest`: verifica 401 para credenciales inválidas y 403 para cuenta deshabilitada.

`GatewaySecurityIntegrationTests` prueba la cadena HTTP de seguridad sin Docker, Auth real ni llamadas externas. Verifica login y refresh públicos, preflight permitido, 401 JSON sin token, Bearer malformado, firma inválida, expiración, issuer incorrecto y rechazo de `type=refresh`; también demuestra que un access token válido atraviesa la capa de seguridad. No se prueba 403 por autoridad porque esta solución no introduce roles nuevos en el Gateway.

## CORS y cabeceras HTTP

### CORS

El único CORS explícito encontrado está en `api-gateway/config/CorsConfig.java` y aplica a `/**`:

- Orígenes: `http://localhost:3000`, `http://localhost:5173`.
- Métodos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- Headers permitidos: `*`.
- Credenciales: habilitadas (`allowCredentials=true`).
- No se evidencian orígenes de producción configurables ni `maxAge`.

### Headers

Auth, Usuarios y Reservas usan Spring Security. Al no deshabilitarse sus writers, se esperan los defaults de Spring Security: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` y headers de no-cache (`Cache-Control`, junto con headers compatibles). HSTS se emite por defecto solo en solicitudes HTTPS. No se encontró CSP configurada explícitamente.

No se atribuyen esos headers al API Gateway ni a Académico: no tienen Spring Security en sus POM/configuración. Tampoco se encontró configuración explícita global de CSP. La presencia efectiva de headers debe confirmarse en un entorno HTTPS desplegado; esta tarea no ejecuta E2E.

## Secretos y credenciales

- `.env.example` documenta `JWT_SECRET` como plantilla y advierte no versionar el valor real.
- Auth, Usuarios y Reservas leen el secreto con `${JWT_SECRET}`.
- Las contraseñas y claves internas se inyectan por variables en `application.yml`/Compose; existen valores por defecto de desarrollo, que no deben usarse en producción.
- CI genera un JWT secret efímero para tests y, en integración, admite un GitHub Secret con fallback de prueba.
- No se identificó un token JWT real ni un secreto de producción versionado. Los valores sensibles no se reproducen en esta evidencia.
- No se encontró escaneo automático de secretos. Se recomienda secret scanning y eliminar fallbacks para despliegues no locales.

## Checklist y conclusión

| Control | Resultado | Evidencia/acción pendiente |
|---|---|---|
| Login y refresh públicos | Conforme | `SecurityConfig` y filtro de Auth |
| Health público | Conforme | Los cinco componentes revisados |
| Prometheus público | Confirmado, revisar exposición | Está abierto; restringir por red en producción |
| Usuarios con JWT | Conforme para 23/23 operaciones esperadas | Roles finos solo en Perfiles; revisar autorización de los demás controladores |
| Reservas/Solicitudes con JWT | Conforme para 25/25 | Autoridades declaradas por patrón |
| Académico/Laboratorios con JWT | Conforme en perímetro Gateway: 63/63 | Limitación: sin validación JWT interna ni roles funcionales propios |
| Endpoints internos | Control alternativo | Seis rutas validan `X-Internal-Api-Key`; mantenerlas fuera de exposición pública |
| 401 unitario | Evidenciado parcialmente | Falta prueba HTTP de cadena completa |
| 403 unitario | Evidenciado para cuenta deshabilitada | Falta prueba de autoridad insuficiente |
| CORS | Parcial | Solo orígenes locales; credenciales habilitadas |
| Headers | Parcial | Defaults en tres servicios; no evidenciados en Gateway/Académico |
| Secretos por entorno | Parcial | Sin secreto real detectado; quedan defaults de desarrollo y falta secret scanning |

La evidencia demuestra **111/111 = 100 %** de cobertura JWT para las operaciones protegidas accesibles por el perímetro externo oficial en `localhost:8080`. El porcentaje es válido porque el Gateway valida transversalmente, las rutas públicas están delimitadas de forma explícita y no existe publicación host de 8081-8084. Persiste una limitación deliberadamente visible: la red interna no ofrece defensa en profundidad completa y Académico no valida JWT internamente.
