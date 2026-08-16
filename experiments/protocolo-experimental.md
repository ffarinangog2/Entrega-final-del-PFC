# Protocolo experimental de procesamiento distribuido

## Objetivo

Comparar, en un subpaso posterior, la línea base implementada con pandas y el
pipeline distribuido implementado con PySpark. La comparación deberá utilizar
los mismos datos de entrada, transformaciones y resultado lógico.

Este documento define el protocolo y la estructura de registro. No ejecuta
experimentos ni contiene resultados.

## Tratamientos

- `pandas-baseline`: ejecución de `spark/baseline.py`.
- `pyspark-pipeline`: ejecución de `spark/pipeline.py`.

Ambos tratamientos deben leer el mismo estado consistente de `reservas_db` y
aplicar el filtro, join, transformación temporal, agregación y clasificación
definidos en el Paso 4.

## Variables

### Variable independiente

Motor de procesamiento: pandas o PySpark.

### Variables dependientes

- tiempo total de ejecución, en milisegundos;
- utilización media y máxima de CPU, en porcentaje;
- memoria residente media y máxima, en MiB;
- filas procesadas;
- rendimiento, expresado en filas procesadas por segundo.

### Variables controladas

- versión y configuración del conjunto de datos;
- consulta y estado de CockroachDB antes de cada tratamiento;
- equipo, sistema operativo y recursos disponibles;
- versiones de Python, pandas, Java y Spark;
- configuración de Spark;
- número de repeticiones y calentamientos;
- procesos externos activos durante la medición.

## Diseño

1. Preparar una instantánea identificable del conjunto de datos.
2. Registrar el entorno y las versiones utilizadas.
3. Ejecutar las iteraciones de calentamiento sin incorporarlas a los
   resultados.
4. Alternar el orden de los tratamientos para reducir el sesgo producido por
   cachés y orden de ejecución.
5. Ejecutar el número configurado de repeticiones para cada tratamiento.
6. Registrar una observación por ejecución conforme a
   `metrics/metric-schema.json`.
7. Conservar también las ejecuciones fallidas, indicando su estado y error.

La recolección real, el cálculo de estadísticos, los gráficos y la comparación
final pertenecen a los siguientes subpasos.

## Condiciones de validez

- Una repetición es válida cuando termina sin error y produce el resultado
  esperado.
- Los tratamientos deben procesar la misma cantidad de filas de entrada.
- No deben modificarse los datos fuente entre tratamientos equivalentes.
- Una interrupción externa debe registrarse como ejecución fallida, no
  eliminarse silenciosamente.
- El rendimiento se calculará posteriormente a partir de filas procesadas y
  tiempo de ejecución; no se registrarán valores estimados.

## Almacenamiento

Los registros futuros se guardarán en `experiments/metrics/results/`. Cada
registro deberá cumplir el esquema JSON y conservar un identificador único de
ejecución, el tratamiento, el conjunto de datos, el entorno y las mediciones
capturadas.

Prometheus y Grafana no se incorporan en este subpaso porque no se identificó
una exigencia explícita en la rúbrica disponible. La estructura JSON permite
integrar posteriormente un recolector externo sin acoplar el protocolo a una
plataforma de observabilidad.
