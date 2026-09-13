# Acta 04 — Trazabilidad, documentación y contratos OpenAPI E4–E6

- **Fecha:** 12/09/2026
- **Hora:** Desde las 19:00
- **Modalidad:** Google Meet
- **Participantes:**
  - Freddy Farinango
  - Isaías Urbina
  - Iván Villamarín
  - Harold Vinueza
- **Estado:** Confirmada

## Objetivo

Integrar la evidencia experimental en la documentación, completar los contratos
OpenAPI y consolidar una única fuente oficial del informe.

## Temas tratados

- Paquete canónico y trazabilidad bidireccional E3/E4.
- Contratos OpenAPI de los cuatro servicios y la fachada Gateway.
- Validador reproducible y gate OpenAPI en CI.
- Fuente documental oficial y conservación de snapshots históricos.

## Decisiones y acuerdos

- Enlazar protocolo, productor, evidencia canónica, análisis y resultado
  interpretado sin alterar el raw ni el análisis E3.
- Versionar los cinco contratos OpenAPI y validarlos contra la implementación.
- Establecer `docs/main.tex` como única fuente oficial acumulativa y conservar
  Entrega 3 y Entrega 4 como documentación histórica.

## Tareas y responsables

| Tarea | Responsable | Estado | Evidencia |
|---|---|---|---|
| Revisar que contratos y fachada respeten los límites arquitectónicos | Freddy Farinango | Completada | `docs/openapi/` |
| Integrar el validador OpenAPI con CI | Isaías Urbina | Completada | `scripts/validar-contratos-openapi.py` |
| Verificar cobertura contractual y resultados E3 | Iván Villamarín | Completada | Matriz E3/E4 y validador OpenAPI |
| Consolidar trazabilidad e informe acumulativo | Harold Vinueza | Completada | `docs/main.tex` y documentación histórica |

## Resultados de la jornada

E4 incorporó evidencia experimental canónica y trazabilidad; E5 completó los
contratos OpenAPI y su gate; E6 unificó la fuente oficial sin eliminar los
documentos históricos.

## Evidencia relacionada

- E4: `cef50709177eee2c41b53f0000f9d936ddb373df`.
- E5: `0bc47c61e54a8173e2ff195012d5d50e7bd975b0`.
- E6: `e0c27e5e6006d1c0af9a3d9efae8b34a61da3afa`.
- `experimentos/resultados/TRAZABILIDAD-E3-E4.md`.
- `docs/openapi/README.md`.
- `docs/main.tex`.

## Seguimiento

Los tres commits quedaron integrados en la rama. La documentación mantiene
separados el SHA experimental, los commits documentales y los snapshots
históricos.
