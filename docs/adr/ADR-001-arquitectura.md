# ADR-001: Arquitectura en capas de academico-laboratorios-service

## Estado
Aceptado

## Contexto
El microservicio `academico-laboratorios-service` (E2/E3) tenía una estructura
plana (controllers, services, entities, repositories mezclados sin separación
clara de capas). La Entrega 4 exige refactorizar a arquitectura en capas con
principios SOLID, sin romper el contrato externo de la API (Guía E4, §5.2).

## Decisión
Se adopta arquitectura hexagonal de 4 capas para las 11 entidades del dominio
(Campus, Bloque, Piso, Laboratorio, Equipo, TipoEquipo, Facultad, Carrera,
Materia, PeriodoLectivo, HorarioAcademico):

- **domain/**: modelos puros sin anotaciones JPA, con comportamiento propio
  (ej. `cambiarEstado()`), puertos (`*RepositoryPort`) como interfaces de
  dominio, y excepciones de negocio (`ResourceNotFoundException`,
  `ConflictException`, `BusinessRuleException`) en `domain/exception/`.
- **application/**: servicios (`*Service` + `*ServiceImpl`) que orquestan la
  lógica de casos de uso contra los puertos de dominio.
- **infrastructure/**: adaptadores de persistencia
  (`entity`, `repository`, `specification`, `mapper`, `adapter`) que
  implementan los puertos definidos en el dominio.
- **presentation/**: controladores REST y DTOs, más el manejo global de
  errores HTTP (`ApiError`, `GlobalExceptionHandler`) en
  `presentation/exception/`.

Patrón GoF aplicado: **Repository** como puerto de dominio en las 11
entidades (interfaz en `domain/port`, implementación en
`infrastructure/persistence/adapter`).

## Consecuencias

**Positivas:**
- El dominio queda desacoplado de JPA/Spring Data; se puede testear con
  dobles de prueba (mocks de los puertos) sin necesidad de base de datos real.
- El contrato externo de la API no cambia, por lo que
  `reservas-solicitudes-service` sigue consumiendo `InternalController` sin
  ajustes.
- La cobertura de pruebas unitarias mejora al mockear puertos de dominio en
  vez de repositorios JPA concretos.

**Negativas:**
- Aumenta el número de archivos por entidad (de 3-4 a aproximadamente 8),
  ampliando la superficie de código a mantener.
- Requiere que nuevos integrantes del equipo se familiaricen con el patrón
  antes de contribuir al servicio.

**Neutrales:**
- No afecta a los artefactos de E1-E3; el cluster CockroachDB y el pipeline
  PySpark siguen operando sin cambios.