# Evidencia E3 canónica

Esta carpeta contiene una selección compacta y versionable de los insumos que
sostienen [`../analisis-e3.json`](../analisis-e3.json). El software medido
corresponde al SHA experimental
`fa7d75ec0f75573938bf46ed6a68f0aee99606ac`; los commits posteriores pueden
incorporar análisis y documentación sin cambiar la identidad del software
ensayado.

Los archivos son copias byte a byte del raw local. No se alteraron resultados,
conteos, manifiestos ni porcentajes. El raw completo permanece fuera de Git por
su volumen y conserva, entre otros derivados, los reportes HTML. Esta selección
no sustituye ni modifica los originales ni las evidencias históricas de SHA
anteriores, que permanecen separadas y etiquetadas como antecedentes.

## Relación con el análisis oficial

| Contenido canónico | Función en la cadena de evidencia |
|---|---|
| `e3_study.json` | Identifica el SHA y la rama comunes del estudio. |
| `e3_*/campaign.json` | Registra metadatos consolidados y las tres repeticiones de cada campaña. |
| `e3_*/rep-NN/manifest.json` | Registra SHA, entorno, comando, estado e integridad de cada repetición. |
| `e3_seguridad/rep-NN/decisiones.csv` | Fuente que `analizar_e3.py` usa para calcular 21/21, errores de decisión y Wilson 95 %. |
| `e3_compatibilidad/rep-NN/<motor>/summary.json` | Fuente que el analizador usa para sumar aprobados, fallidos, omitidos y flaky por motor. |
| `e3_mantenibilidad/rep-NN/metricas.csv` | Fuente que el analizador usa para medias, desviación, IC95, umbrales y decisiones. |
| `e3_mantenibilidad/rep-NN/<servicio>/report/jacoco.csv` | Reporte fuente compacto de cobertura para los cinco servicios Java. |
| `e3_mantenibilidad/rep-NN/web/report/coverage-summary.json` | Reporte fuente compacto de cobertura Web. |
| `e3_mantenibilidad/rep-NN/android/report/jacocoTestReport.xml` | Reporte fuente compacto de cobertura Android. |
| `MANIFEST-SHA256.txt` | Hash SHA-256 de cada archivo incluido en este paquete, salvo el propio manifiesto. |

`analizar_e3.py` no necesita los `playwright.json` para producir
`analisis-e3.json`; por eso no se incluyeron. Tampoco se copiaron HTML, logs ni
otros derivados voluminosos. La trazabilidad completa está en
[`../TRAZABILIDAD-E3-E4.md`](../TRAZABILIDAD-E3-E4.md).

## Resultado desfavorable preservado

La selección incluye sin ocultarlo el resultado Android de
38,34070796460177 % de líneas frente al umbral de 70 %. Por esta única puerta,
la decisión global experimental de mantenibilidad es **NO CUMPLE**.
