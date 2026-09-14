# Registro retrospectivo 03 — Experimentación ISO 25010 y preparación/cierre de E3

- **Fecha verificable de registro:** 12/09/2026
- **Commit de consolidación:** `1ce91ba05e8048fda75a8479d368684a2c338430`
- **Periodo técnico asociado:** 11–12/09/2026, según las fechas de los commits
  citados.
- **Naturaleza:** registro retrospectivo de decisiones y trabajos verificables;
  no prueba una reunión celebrada el 11/09/2026.
- **Participantes consignados en el registro:**
  - Freddy Farinango
  - Isaías Urbina
  - Iván Villamarín
  - Harold Vinueza

Git permite verificar la preparación, el SHA experimental y los resultados
versionados. No permite demostrar una hora, modalidad o asistencia sincrónica;
las responsabilidades se conservan como atribución retrospectiva.

## Objetivo

Coordinar la preparación reproducible de las campañas de Mantenibilidad,
Seguridad y Compatibilidad y su posterior cierre experimental.

## Temas tratados

- Instrumental y productor de las campañas E3.
- Tres repeticiones y conservación de manifiestos y resultados raw.
- Separación entre protocolo, ejecución, análisis e interpretación.
- Identificación del SHA del software medido.

## Decisiones consolidadas retrospectivamente

- Ejecutar las campañas con los criterios y umbrales prerregistrados.
- Mantener separados el SHA experimental y los commits documentales posteriores.
- Conservar resultados favorables y desfavorables sin reinterpretarlos durante
  la documentación.

## Tareas y responsables

| Tarea | Responsable | Estado | Evidencia |
|---|---|---|---|
| Verificar la correspondencia de las campañas con la arquitectura | Freddy Farinango | Completada | `experimentos/protocolo-e4.md` |
| Integrar el instrumental con los componentes ejecutados | Isaías Urbina | Completada | `experimentos/ejecutar_e3.py` |
| Revisar criterios, métricas y análisis | Iván Villamarín | Completada | `experimentos/analizar_e3.py` y `analisis-e3.json` |
| Mantener la trazabilidad documental de la campaña | Harold Vinueza | Completada posteriormente | Resumen y matriz E3/E4 |

## Resultados verificables

Se prepararon las campañas experimentales de Mantenibilidad, Seguridad y
Compatibilidad. El software medido quedó identificado posteriormente con el SHA
`fa7d75ec0f75573938bf46ed6a68f0aee99606ac`, y los resultados finales se
registraron sin cambiar sus criterios ni reinterpretarlos en esta acta.

## Evidencia relacionada

- Preparación principal: `fdc8e6827ae74b2b439acae84776b3e2f93b3e4e`.
- SHA experimental oficial posterior: `fa7d75ec0f75573938bf46ed6a68f0aee99606ac`.
- `experimentos/protocolo-e4.md`.
- `experimentos/resultados/analisis-e3.json`.

## Seguimiento

El registro fue consolidado el 12/09/2026. El análisis y la selección canónica
de evidencia fueron incorporados después en
E4. La interpretación oficial continúa en los documentos de resultados E3/E4;
esta acta no sustituye esos resultados.
