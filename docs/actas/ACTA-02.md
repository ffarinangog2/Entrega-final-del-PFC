# Acta 02 — Integración, pruebas y CI/CD

- **Fecha:** 28/08/2026
- **Hora:** Desde las 19:00
- **Modalidad:** Google Meet
- **Participantes:**
  - Freddy Farinango
  - Isaías Urbina
  - Iván Villamarín
  - Harold Vinueza
- **Estado:** Confirmada

## Objetivo

Coordinar la validación automatizada de backend, web y móvil, y separar las
responsabilidades de integración continua y despliegue continuo.

## Temas tratados

- Pruebas unitarias e integración de los servicios backend.
- Pruebas web y móvil dentro del pipeline.
- Contratos Pact para interacciones seleccionadas.
- Publicación condicionada a gates y separación entre CI y CD.

## Decisiones y acuerdos

- Mantener validaciones de backend, web, móvil e integración como gates del
  pipeline.
- Conservar Pact como evidencia de interacciones consumidor-proveedor
  seleccionadas.
- Separar integración continua y despliegue para que la publicación no sustituya
  las validaciones previas.

## Tareas y responsables

| Tarea | Responsable | Estado | Evidencia |
|---|---|---|---|
| Revisar la coherencia arquitectónica del pipeline | Freddy Farinango | Completada | Workflows versionados |
| Integrar las validaciones web, móvil y de servicios | Isaías Urbina | Completada | `d0bcc23863cdd4bc1646a34458db52d2563ac283` |
| Verificar pruebas y contratos Pact | Iván Villamarín | Completada | `30a55723cb7eb22a59669946de82984e39b1ca09` |
| Documentar la separación y sus evidencias | Harold Vinueza | Completada | `.github/workflows/ci.yml`, `.github/workflows/cd.yml` |

## Resultados de la jornada

El repositorio incorporó validaciones para las distintas capas y separó el
pipeline de integración del proceso de despliegue.

## Evidencia relacionada

- Commit principal: `e439b35d69f01307f529152589a0d87fa09a9d6e`.
- Pipeline integral: `d0bcc23863cdd4bc1646a34458db52d2563ac283`.
- Contratos Pact: `30a55723cb7eb22a59669946de82984e39b1ca09`.
- `.github/workflows/ci.yml` y `.github/workflows/cd.yml` en esos hitos.

## Seguimiento

Los gates evolucionaron posteriormente hacia el workflow acumulativo de CI/CD.
Los contratos Pact permanecieron como complemento de los contratos OpenAPI
incorporados en E5.
