# Acta 01 — Planificación y arquitectura del sistema distribuido

- **Fecha:** 20/08/2026
- **Hora:** Desde las 19:00
- **Modalidad:** Google Meet
- **Participantes:**
  - Freddy Farinango
  - Isaías Urbina
  - Iván Villamarín
  - Harold Vinueza
- **Estado:** Confirmada

## Objetivo

Coordinar la consolidación arquitectónica del sistema distribuido, sus límites
por microservicio y la documentación de las decisiones técnicas.

## Temas tratados

- Arquitectura por capas del servicio académico-laboratorios.
- Separación de responsabilidades entre Auth, Usuarios, Académico-Laboratorios,
  Reservas-Solicitudes y API Gateway.
- Uso de ADR para conservar las decisiones arquitectónicas verificables.
- Distribución general del trabajo según los roles del equipo.

## Decisiones y acuerdos

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

## Resultados de la jornada

Quedó establecida una base documental para la arquitectura por capas y una
distribución de responsabilidades compatible con los cinco componentes del
sistema.

## Evidencia relacionada

- Commit principal: `ea3a5724113ae08108f447881cf2d05eda05aac2`.
- `docs/adr/ADR-001-arquitectura.md`.
- `docs/adr/ADR-003-fragmentacion.md`.
- `docs/adr/ADR-004-consenso-raft.md`.

## Seguimiento

El ADR-001 quedó versionado el 20/08/2026. Los ADR posteriores y el informe
acumulativo conservaron la arquitectura y sus limitaciones como decisiones
trazables.
