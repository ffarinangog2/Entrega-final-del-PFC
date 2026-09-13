# Evidencia ARBITER E4

Este directorio conserva la evidencia versionada de la campaña ARBITER ejecutada
sobre el SHA experimental
`e43967eda31410b7fef060cfc7903cea94efbe96`.

La campaña planificó **130 corridas**. El estado registrado se descompone así:

- **123 corridas COMPLETED**;
- **7 corridas FAILED**.

Por tanto, `130 planificadas = 123 completadas + 7 fallidas`.

De las 123 corridas completadas:

- **99** no fueron excluidas y se utilizaron en el análisis;
- **24** fueron excluidas del análisis.

La evidencia se encuentra en [`campaign/`](campaign/) y está organizada de la
siguiente manera:

- [`campaign/manifest/checkpoint.json`](campaign/manifest/checkpoint.json):
  manifiesto de las 130 corridas, su estado y su exclusión del análisis;
- [`campaign/summary/runs.json`](campaign/summary/runs.json) y
  [`campaign/summary/runs.csv`](campaign/summary/runs.csv): resumen estructurado
  de las corridas;
- [`campaign/analysis/comparisons.json`](campaign/analysis/comparisons.json):
  **24 comparaciones estadísticas**;
- [`campaign/analysis/oracle/`](campaign/analysis/oracle/): **120 archivos de
  oráculo**;
- [`campaign/raw/`](campaign/raw/): evidencia raw conservada por corrida;
- [`campaign/SHA256SUMS`](campaign/SHA256SUMS): integridad SHA-256 de los
  **315 archivos** incluidos en el manifiesto de hashes. La verificación se
  completa sin errores.

Los tests unitarios del harness validan el instrumental, pero no sustituyen la
evidencia experimental registrada en `campaign/`.
