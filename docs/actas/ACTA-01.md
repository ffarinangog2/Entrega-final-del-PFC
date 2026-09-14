# Registro retrospectivo 01 — Planificación y arquitectura del sistema distribuido

- **Fecha verificable de registro:** 12/09/2026
- **Commit de consolidación:** `1ce91ba05e8048fda75a8479d368684a2c338430`
- **Hito técnico asociado:** 20/08/2026, commit
  `ea3a5724113ae08108f447881cf2d05eda05aac2`
- **Naturaleza:** registro retrospectivo de decisiones y trabajos verificables;
  no prueba una reunión celebrada el 20/08/2026.
- **Participantes consignados en el registro:**
  - Freddy Farinango
  - Isaías Urbina
  - Iván Villamarín
  - Harold Vinueza

La fecha, hora y modalidad de una reunión sincrónica no están acreditadas por
Git o GitHub. Los nombres y responsabilidades se conservan como atribución del
registro consolidado; los commits y artefactos citados acreditan los resultados,
no una asistencia a Google Meet.

## Objetivo

Coordinar la consolidación arquitectónica del sistema distribuido, sus límites
por microservicio y la documentación de las decisiones técnicas.

## Temas tratados

- Arquitectura por capas del servicio académico-laboratorios.
- Separación de responsabilidades entre Auth, Usuarios, Académico-Laboratorios,
  Reservas-Solicitudes y API Gateway.
- Uso de ADR para conservar las decisiones arquitectónicas verificables.
- Distribución general del trabajo según los roles del equipo.

## Decisiones consolidadas retrospectivamente

- Mantener los límites de los microservicios y documentar sus decisiones sin
  trasladar responsabilidades entre dominios de forma implícita.
- Adoptar una arquitectura hexagonal de cuatro capas para el servicio
  académico-laboratorios, según el ADR-001.
- Usar documentación versionada y commits como trazabilidad de las decisiones
  implementadas.

## Tareas y responsables

| Tarea | Responsable | Estado | Evidencia |
|---|---|---|---|
| Consolidar la decisión de arquitectura por capas | Freddy Farinango | Completada | `docs/adr/ADR-001-arquitectura.md` |
| Coordinar la integración técnica entre componentes | Isaías Urbina | Completada posteriormente | Historial de integración de la rama |
| Revisar que la estructura permita pruebas aisladas | Iván Villamarín | Completada posteriormente | Pruebas de servicios y gates de calidad |
| Versionar la decisión arquitectónica | Harold Vinueza | Completada | Commit `ea3a5724113ae08108f447881cf2d05eda05aac2` |

## Resultados verificables

Quedó establecida una base documental para la arquitectura por capas y una
distribución de responsabilidades compatible con los cinco componentes del
sistema.

## Evidencia relacionada

- Commit principal: `ea3a5724113ae08108f447881cf2d05eda05aac2`.
- `docs/adr/ADR-001-arquitectura.md`.
- `docs/adr/ADR-003-fragmentacion.md`.
- `docs/adr/ADR-004-consenso-raft.md`.

## Seguimiento

El ADR-001 quedó versionado el 20/08/2026. Este registro fue creado realmente
el 12/09/2026; los ADR posteriores y el informe
acumulativo conservaron la arquitectura y sus limitaciones como decisiones
trazables.
