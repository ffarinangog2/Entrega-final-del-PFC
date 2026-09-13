# Trazabilidad bidireccional de evidencias E3–E4

Esta matriz permite recorrer cada resultado desde su objetivo hasta la evidencia
y volver desde el documento a su productor. Los SHA identifican el software
medido; commits documentales posteriores no sustituyen esa identidad.

| Característica | Pregunta/objetivo | Protocolo | Productor | Raw esperado o efectivo | Análisis | Resultado | Documento que lo interpreta | SHA medido |
|---|---|---|---|---|---|---|---|---|
| Eficiencia | PI1: p95 nominal <500 ms; p99 secundario <750 ms mediante límite superior IC95 | [`../protocolo-e4.md`](../protocolo-e4.md), eficiencia nominal | `ejecutar_iso25010.ps1`, `registrar_iso25010.py` | `raw/eficiencia_nominal_50u_5m/rep-01..10/`; filas GET preservadas; `Aggregated` conservado como histórico | `analizar_iso25010.py` lee los raws y delimita por identidad del request; `iso25010.csv` conserva el antecedente agregado | p95 CUMPLE; p99 CUMPLE; análisis histórico agregado preservado | [`RESUMEN-ISO25010-E4.md`](RESUMEN-ISO25010-E4.md), informe E4 | `a47f0441f644bea5f52944b7a11216f37b2242de` |
| Fiabilidad | PI2: tasa HTTP 5xx nominal <1 %; disponibilidad temporal separada | [`../protocolo-e4.md`](../protocolo-e4.md), fiabilidad nominal | `ejecutar_iso25010.ps1`, `registrar_iso25010.py` | `raw/fiabilidad_nominal_50u_1h/rep-01..10/`; evidencia seleccionada versionada | `analizar_iso25010.py` → `iso25010.csv` | 5xx CUMPLE; disponibilidad NO CONCLUYENTE | [`RESUMEN-ISO25010-E4.md`](RESUMEN-ISO25010-E4.md), informe E4 | `061a1050a94e1bd30d81b30c47c7e818005a33bb` |
| Seguridad | 21 decisiones dinámicas correctas; cero falsos permitidos/rechazados y flaky | [`../protocolo-e4.md`](../protocolo-e4.md), prerregistro E3 | `ejecutar_e3.py`, `e3_instrumental.py` | [`evidencia-e3-canonica/e3_seguridad/`](evidencia-e3-canonica/e3_seguridad/): manifiestos y decisiones; selección canónica del raw completo no versionado | `analizar_e3.py` → [`analisis-e3.json`](analisis-e3.json) | 21/21; Wilson [0,845360981013798; 1]; CUMPLE en matriz | Resumen, `docs/iso25010/seguridad-ivan.md`, informe E4 | `fa7d75ec0f75573938bf46ed6a68f0aee99606ac` |
| Mantenibilidad | Cobertura por componente y puertas secundarias sobre tres ejecuciones | [`../protocolo-e4.md`](../protocolo-e4.md), prerregistro E3 | `ejecutar_e3.py`, `e3_instrumental.py` | [`evidencia-e3-canonica/e3_mantenibilidad/`](evidencia-e3-canonica/e3_mantenibilidad/): métricas y reportes fuente compactos; selección canónica del raw completo no versionado | `analizar_e3.py` → [`analisis-e3.json`](analisis-e3.json) | Todos cumplen salvo Android 38,3407 %; global NO CUMPLE | Resumen, `docs/iso25010/mantenibilidad-usuarios.md`, informe E4 | `fa7d75ec0f75573938bf46ed6a68f0aee99606ac` |
| Compatibilidad | Suite completa aprobada en Chromium, Firefox y WebKit, sin fallidos, omitidos o flaky | [`../protocolo-e4.md`](../protocolo-e4.md), prerregistro E3 | `ejecutar_e3.py`, `e3_instrumental.py` | [`evidencia-e3-canonica/e3_compatibilidad/`](evidencia-e3-canonica/e3_compatibilidad/): manifiestos y resúmenes por motor; selección canónica del raw completo no versionado | `analizar_e3.py` → [`analisis-e3.json`](analisis-e3.json) | 24/24 por motor; Wilson [0,862023795269197; 1]; CUMPLE en suite | Resumen, `docs/iso25010/compatibilidad.md`, informe E4 | `fa7d75ec0f75573938bf46ed6a68f0aee99606ac` |

### Replicación de mantenibilidad

`rep-01`, `rep-02` y `rep-03` son ejecuciones separadas en ventanas UTC no
solapadas (05:38:47–05:52:30, 05:53:48–06:05:14 y 06:07:00–06:18:59). Cada una
tiene manifiesto propio. Los informes Android conservados tienen hashes
`3152520e…`, `689c5141…` y `07d393d0…`, y sesiones JaCoCo
`DESKTOP-N45EJGV-f3ab9b30`, `DESKTOP-N45EJGV-b268151` y
`DESKTOP-N45EJGV-f0c2743e`, respectivamente. La tabla completa está en el
[`README` de la evidencia canónica](evidencia-e3-canonica/README.md).

Las tres ejecuciones produjeron 38,34070796460177 % para Android. La igualdad
produce media = 38,34070796460177 %, `sample_sd = 0` e IC95 =
[38,34070796460177; 38,34070796460177]. Es reproducibilidad determinista de la
misma suite sobre el mismo SHA, no una demostración estadística de independencia
ni de precisión fuerte. Se preservan el umbral de 70 % y la decisión **NO
CUMPLE**.

## Lectura inversa desde los artefactos

- `analisis-e3.json` se obtiene de las tres carpetas raw E3 mediante
  `analizar_e3.py`; sus decisiones se publican en el resumen, los tres documentos
  auxiliares y el informe E4.
- Cada `manifest.json` E3 identifica SHA, campaña, repetición, entorno, comando y
  estado. `e3_study.json` fija la identidad común del software medido.
- `iso25010.csv` enlaza las campañas anteriores de eficiencia y fiabilidad con
  sus repeticiones raw, su analizador, el resumen y el informe.
- Los resultados de `cd61b643...`, `e43967e...` y otros SHA anteriores son
  antecedentes históricos. No se combinan con las decisiones oficiales E3.

## Estado de conservación

El análisis E3 y la selección
[`evidencia-e3-canonica/`](evidencia-e3-canonica/) son versionables. El raw E3
completo permanece en el entorno de ejecución e ignorado por Git debido a su
volumen. El paquete canónico conserva los insumos mínimos y reportes fuente
compactos; no sustituye ni altera los originales históricos.
