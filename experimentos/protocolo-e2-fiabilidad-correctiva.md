# Prerregistro correctivo E2: fiabilidad nominal con renovación de sesión

**Fecha de prerregistro:** 2026-09-12 (America/Guayaquil)  
**Estado:** definido antes de ejecutar la campaña correctiva.

> Este documento prerregistra una campaña correctiva posterior. No sustituye ni
> reescribe el protocolo ni la evidencia histórica.

## Antecedente y motivo

La campaña histórica de fiabilidad fue ejecutada con el SHA experimental
`061a1050a94e1bd30d81b30c47c7e818005a33bb`. Su `locustfile.py` obtenía un
`accessToken` al inicio de cada usuario y no lo renovaba. El access token tiene
un TTL configurado de 900 s; tras aproximadamente 15 minutos, las solicitudes
expiradas producían HTTP 401 en el Gateway y disminuían la carga efectiva que
alcanzaba Reservas/Solicitudes.

La campaña histórica y todos sus raws permanecen en
`experimentos/resultados/raw/fiabilidad_nominal_50u_1h/`. No serán eliminados,
sobrescritos ni reinterpretados como si hubieran usado renovación. Sus
resultados oficiales no se sustituyen antes de completar y analizar la campaña
correctiva.

La única modificación deliberada del escenario es la gestión de sesión:
Locust utilizará el mecanismo real `POST /api/v1/auth/refresh`, renovará antes
de la expiración y permitirá un único refresh y un único reintento ante un 401.
No se modifica el TTL ni la configuración JWT del sistema.

## Diseño congelado antes de medir

| Parámetro | Regla prerregistrada |
|---|---|
| Repeticiones | 10 repeticiones independientes. |
| Duración | 1 hora por repetición. |
| Carga | 50 usuarios y `spawn-rate` de 10 usuarios/s, igual que el protocolo anterior. |
| Escenario | Misma infraestructura, datos y consultas nominales del protocolo anterior, salvo la corrección de renovación de sesión descrita aquí. |
| Conservación | r1 y r10 se conservan y publican; el análisis estadístico principal usa r2--r9. |
| IC95 | `x̄ ± t(0.975, 7) × s / sqrt(8)`, con `t = 2.364624251`, sin cambiar la regla previa. |
| Métrica primaria | Porcentaje de respuestas HTTP 5xx sobre las solicitudes GET de negocio. |
| Umbral y decisión | HTTP 5xx `<1 %`; cumple únicamente si el límite superior del IC95 de r2--r9 es menor que 1 %. |
| Latencia | p95 y p99 se calculan únicamente sobre los GET de negocio. Se informan sin convertirlos en un criterio nuevo de decisión de E2. |

## Población experimental

La población de negocio está definida por la identidad del request:

- `GET /api/v1/reservas`;
- `GET /api/v1/reservas/{id}`, cuando tenga observaciones.

`POST /api/v1/auth/login` y `POST /api/v1/auth/refresh` son tráfico de sesión.
Se preservarán y contabilizarán por separado, pero no formarán parte del
denominador HTTP 5xx de negocio ni de los percentiles p95/p99 de negocio.

No se excluirá ningún GET por su código o resultado. Los GET con respuesta 200,
401, 500 o cualquier otro estado pertenecen a la población. No se filtrarán
observaciones por éxito, fallo o latencia. Si ambas identidades GET tienen
observaciones, los percentiles sólo se combinarán cuando exista una distribución
raw que permita calcularlos sobre el conjunto de observaciones; no se promediarán
percentiles de endpoints.

Se publicarán por separado, como mínimo:

- cantidad total de GET de negocio y sus respuestas por estado HTTP;
- cantidad y tasa de HTTP 401 de los GET de negocio;
- cantidad y tasa de HTTP 5xx de los GET de negocio;
- cantidad, estado y fallos de login;
- cantidad, estado y fallos de refresh;
- p95 y p99 de los GET de negocio.

## Gestión de sesión prerregistrada

Cada usuario conservará `accessToken`, `refreshToken` e instante de expiración.
Después del login inicial renovará mediante `/api/v1/auth/refresh` antes de que
expire el access token. Una respuesta 401 de un GET permite exactamente un
refresh y un único reintento del mismo GET. El refresh rotado reemplaza tanto el
access token como el refresh token y actualiza el encabezado `Authorization`.

Todo refresh fallido se registrará explícitamente como fallo. No se convertirá
en éxito, no iniciará un bucle de reintentos y no se eliminará del análisis.

## Smoke técnico previo

Antes de las diez repeticiones oficiales se realizará un único smoke técnico,
con una duración prevista de aproximadamente 20--25 minutos, suficiente para
superar el TTL de 900 s del access token.

El smoke tendrá únicamente finalidad técnica. Verificará que:

- ocurre al menos un refresh;
- el `accessToken` se renueva;
- el `refreshToken` rotado se utiliza correctamente en la renovación siguiente;
- los GET continúan ejecutándose después de `t = 900 s`;
- los GET continúan llegando a Reservas/Solicitudes después del minuto 15;
- no se reproduce el patrón histórico de pérdida masiva de carga autenticada
  asociado a la expiración del access token.

El smoke no forma parte de las diez repeticiones oficiales, no se numera como
r1--r10 y no sustituye ninguna repetición oficial. Sus observaciones no entran
en r2--r9, medias, desviaciones, IC95 ni decisiones de cumplimiento.

Sus archivos se almacenarán separadamente en:

`experimentos/resultados/raw/fiabilidad_nominal_50u_1h_refresh_smoke/`

No se mezclarán con la evidencia oficial ubicada en
`experimentos/resultados/raw/fiabilidad_nominal_50u_1h_refresh/`.

La condición `401 = 0` no será un criterio de éxito del smoke. Todo 401 se
preservará y documentará. El criterio técnico será demostrar que, después del
TTL, no reaparece el patrón histórico de pérdida masiva de carga autenticada.

Tanto para el smoke como para cada repetición oficial se documentarán por
separado las fases `t < 900 s` y `t >= 900 s`, incluyendo como mínimo:

- GET intentados;
- GET exitosos;
- respuestas HTTP 401;
- respuestas HTTP 5xx;
- refresh intentados y sus resultados;
- evidencia de llegada de los GET a Reservas/Solicitudes.

La comparación con el aproximadamente 75 % de HTTP 401 observado en la campaña
histórica será descriptiva y no constituye un nuevo umbral estadístico de
aceptación.

## Evidencia y ubicación nueva

La nueva campaña se almacenará exclusivamente en:

`experimentos/resultados/raw/fiabilidad_nominal_50u_1h_refresh/`

Cada repetición usará una carpeta nueva `rep-01` a `rep-10`. No se escribirá en
`experimentos/resultados/raw/fiabilidad_nominal_50u_1h/`.

Para cada repetición se conservarán los CSV completos de Locust (estadísticas,
historial, fallos y excepciones), informe y log de ejecución, metadatos, estado
de infraestructura, evidencia del Gateway y de Reservas/Solicitudes, consultas
y resultados de Prometheus, y cualquier salida necesaria para reconstruir los
conteos. Los metadatos registrarán, al menos, inicio y fin UTC, duración, usuarios,
spawn rate, versiones, host, SHA experimental exacto y estado del árbol utilizado.
Al cerrar la campaña se generará un manifiesto SHA-256 de la evidencia preservada.

## Comprobaciones operacionales de validez

Antes de aceptar una repetición para el análisis se comprobará y documentará:

1. que la ejecución cubrió aproximadamente 3600 s y que los tiempos exactos de
   inicio, fin y duración quedaron registrados;
2. que se ejecutó al menos un refresh durante la repetición y se preservaron sus
   conteos, estados y fallos;
3. que los GET de negocio continuaron después del minuto 15;
4. que la evidencia del Gateway y de Reservas/Solicitudes demuestra que esos GET
   posteriores al minuto 15 siguieron alcanzando el servicio objetivo;
5. que todos los 401 y 5xx quedaron preservados y se informan sin filtrar;
6. que los archivos raw y metadatos necesarios están completos y verificables.

La condición `401 = 0` no es criterio de eliminación ni de validez. Si aparecen
401 reales, la repetición y sus observaciones se conservan y se analizan. Toda
desviación operacional se informa antes de decidir si afecta la comparabilidad;
no se descarta una repetición por producir un resultado desfavorable.

## Secuencia posterior

Este prerregistro no contiene resultados de la campaña correctiva. Después de
ejecutar las diez repeticiones se verificará primero la integridad de la evidencia,
se aplicarán las reglas anteriores y sólo entonces se decidirá si corresponde
actualizar el resultado oficial E2. La evidencia y la interpretación histórica
permanecerán visibles junto con cualquier resultado correctivo.
