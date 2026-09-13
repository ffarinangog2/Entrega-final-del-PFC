# Cierre metodológico E2: fiabilidad, sesión y alcance de la evidencia

**Fecha de cierre documental:** 2026-09-13 (America/Guayaquil)
**Alcance:** consolidación trazable de evidencia ya producida; no se ejecutaron
nuevas repeticiones ni se modificaron raws o resultados históricos.

## Dictamen

La campaña histórica conserva las diez repeticiones de una hora. Durante su
análisis se detectó que el generador de carga no renovaba el JWT de acceso de
900 s, causando HTTP 401 a partir de aproximadamente 15 minutos. Este hallazgo
se conserva y no se excluye de los raws.

La causa fue corregida mediante renovación preventiva y fallback de refresh.
La corrección fue verificada mediante un smoke de más de 15 minutos y una
repetición completa de una hora con tráfico autenticado sostenido.

Estas afirmaciones no convierten la campaña histórica en una campaña con
refresh ni convierten los intentos correctivos fallidos en repeticiones válidas.
La evidencia disponible permite defender el cálculo histórico acotado de HTTP
5xx y la eficacia técnica de la renovación. No permite afirmar que existen diez
repeticiones correctivas completas ni calcular un nuevo IC95 correctivo.

## Clasificación de la evidencia

### Campaña histórica completa

- Ruta: `raw/fiabilidad_nominal_50u_1h/rep-01` a `rep-10`.
- SHA experimental: `061a1050a94e1bd30d81b30c47c7e818005a33bb`.
- Diseño ejecutado: diez ventanas de aproximadamente 3.600 s, 50 usuarios y
  `spawn-rate` 10 usuarios/s.
- Población de negocio: `GET /api/v1/reservas` y
  `GET /api/v1/reservas/{id}`. Ningún GET se excluye por su estado HTTP.
- Censo reproducido desde `locust_stats.csv` y `locust_failures.csv`: 889.868
  GET, 668.367 respuestas HTTP 401 y 612 respuestas HTTP 5xx.
- El primer 401 queda acotado por el historial acumulado entre 902,396 s y
  929,048 s según la repetición. La aparición coincide con el TTL de 900 s más
  el escalonamiento de los usuarios.
- Las diez ejecuciones completaron su duración y conservaron evidencia, pero
  quedaron experimentalmente debilitadas para representar una hora sostenida
  de carga autenticada sobre Reservas/Solicitudes: tras expirar los tokens, una
  gran parte de los GET fue rechazada antes de ejecutar la operación de negocio.

### Defecto experimental detectado

El `locustfile.py` histórico hacía login una vez por usuario y reutilizaba el
access token sin renovarlo. El TTL configurado era 900 s. Los 668.367 HTTP 401
representan el 75,108555426 % de los GET históricos y no se eliminan del
denominador, de los fallos de Locust ni de la evidencia. Son una amenaza a la
validez de la carga efectiva, no errores de servidor 5xx.

### Corrección aplicada

El harness actual conserva `accessToken`, `refreshToken` y expiración; ejecuta
refresh preventivo, reemplaza ambos tokens cuando hay rotación y permite como
fallback un solo refresh y un solo reintento ante 401. Login y refresh conservan
nombres Locust separados de los GET de negocio. No se alargó el TTL ni se
alteró la configuración JWT productiva.

### Evidencia correctiva

El smoke dispone de una selección canónica versionable en
`evidencia-e2/smoke-refresh-25m/`, copiada byte a byte desde
`resultados/raw/fiabilidad_nominal_50u_1h_refresh_smoke/`. Su manifiesto local
verifica los siete archivos seleccionados. Las carpetas de r1 y los intentos de
r2 no están incorporadas a este checkout; sus campos se limitan al registro de
ejecución aportado para este cierre y no se completan con valores supuestos.

| Evidencia | Duración | Refresh | HTTP 401 | Estado preservado | Uso permitido |
|---|---:|---:|---:|---|---|
| Histórica r1–r10 | 10 × ~1 h | No | 668.367 | Completa, afectada por expiración | Diagnóstico y estadística histórica acotada de 5xx |
| Smoke correctivo ([`evidencia-e2/smoke-refresh-25m/`](evidencia-e2/smoke-refresh-25m/)) | 1.500 s / 25 min | 50 correctos, 0 fallidos | 0 | Válido como smoke técnico | Demuestra renovación y sesión autenticada más allá del TTL; no entra en r1–r10 ni en IC95 |
| Correctiva r1 (`raw/fiabilidad_nominal_50u_1h_refresh/rep-01/`) | 1 h; 88.248 GET de negocio | Sí | No revalidable en este checkout | `execution_completed=true` y manifiesto válido según su registro | Evidencia correctiva de una hora; una sola muestra, no un nuevo estudio de diez repeticiones |
| Correctiva r2, intento 1 (`rep-02/`) | No disponible en este checkout | No determinado aquí | No determinado aquí | Inválido y conservado | Trazabilidad del intento; no entra en resultados oficiales |
| Correctiva r2, intento 2 (`rep-02-attempt-02/`) | No disponible en este checkout | No determinado aquí | No determinado aquí | Inválido y conservado | Trazabilidad del intento; no entra en resultados oficiales |
| Correctiva r2, intento 3 (`rep-02-attempt-03/`) | ~1 h; 87.546 GET de negocio | Sí | No revalidable en este checkout | Inválido por falso negativo instrumental y conservado | Diagnóstico del harness; no se reclasifica ni entra en resultados oficiales |

El intento 3 de r2 completó la duración y mantuvo el entorno, pero el validador
`validate_prometheus_arrival()` exigía que una serie HTTP de negocio cubriera
desde `start + 15 s`. Una serie de contador etiquetada por método/URI puede nacer
en el primer request de negocio y no necesariamente al inicio de la ventana; esa
condición produjo un falso negativo instrumental. El intento permanece inválido
y su `phase-summary` no se reconstruye retroactivamente.

### Limitaciones restantes

- Solo existe una repetición correctiva de una hora declarada válida. No hay
  diez repeticiones correctivas válidas y no se calcula una media o IC95 nuevo.
- El smoke es una comprobación técnica y está excluido del análisis estadístico.
- El intento 3 de r2 no se reutiliza como observación oficial aunque su fallo
  conocido sea instrumental.
- Los artefactos de r1 y r2 no están presentes en este checkout; su incorporación
  futura deberá preservar rutas, intentos y manifiestos originales para permitir
  verificación independiente.

### Selección canónica del smoke

La selección contiene exclusivamente `start_utc.txt`, `end_utc.txt`,
`experimental_sha.txt`, `locust.log`, `locust_stats.csv`,
`locust_failures.csv` y `locust_exceptions.csv`, más `SHA256SUMS.txt`, que
verifica los siete archivos sin incluirse a sí mismo. Los hashes de cada copia
coinciden con los del raw original.

Los archivos fijan inicio `2026-09-13T04:32:45Z`, fin
`2026-09-13T04:57:45Z`, duración de 1.500 s y SHA experimental
`164914219f304d17d1a3e9a6989de4e3e9d0a17c`. Locust registra 50 usuarios,
ejecución hasta el límite temporal, 50 login sin fallos, 50 refresh sin fallos,
27.207 GET de listado con 151 fallos y 9.124 GET por id con 68 fallos. Los 219
fallos GET preservados son exclusivamente HTTP 500; no existe HTTP 401 en
`locust_failures.csv`. Esta evidencia se usa únicamente para verificar que el
mecanismo de refresh conserva autenticación durante una ejecución que supera el
TTL de 900 s; no sustituye ninguna repetición histórica u oficial.

## Métricas que no deben mezclarse

### 1. HTTP 401 de autenticación

Los 401 pertenecen a los GET intentados y permanecen en su población. Se
reportan como defecto experimental histórico porque revelan pérdida de sesión y
reducción de carga de negocio efectiva después del TTL. No se transforman en
5xx ni se excluyen para mejorar el resultado.

### 2. HTTP 5xx

Para cada repetición se calculó `100 × GET con estado 5xx / GET de negocio`.
Sobre las repeticiones centrales r2–r9:

| Métrica | n | Media | s muestral | IC95 | Umbral | Decisión matemática |
|---|---:|---:|---:|---|---:|---|
| Tasa HTTP 5xx histórica | 8 | 0,061349491 % | 0,035030537 % | [0,032063230 %; 0,090635753 %] | <1 % | CUMPLE |

Los valores por repetición central son 0,022421776 %, 0,043802999 %,
0,052873149 %, 0,102206997 %, 0,064156677 %, 0,108823694 %,
0,083047157 % y 0,013463480 %. El cociente es matemáticamente reproducible,
pero la inferencia experimental queda limitada porque los 401 redujeron durante
gran parte de la hora las operaciones que alcanzaron el servicio objetivo.

El análisis histórico publicado que utilizó la fila `Aggregated` incluía además
los 50 logins por repetición en el denominador. Se conserva en los documentos
históricos; no es el valor adoptado aquí para la población formal de GET de
negocio.

### 3. Población business GET

Se incluyen por identidad, con independencia del resultado:

- `GET /api/v1/reservas`;
- `GET /api/v1/reservas/{id}`, cuando tenga observaciones.

Se preservan como tráfico de sesión, pero se excluyen del denominador y de los
percentiles de negocio por pertenecer a Auth:

- `POST /api/v1/auth/login`;
- `POST /api/v1/auth/refresh`.

No se filtra un GET por responder 200, 401, 500 u otro código.

## Amenazas a la validez y acciones correctivas

La expiración del JWT constituye una amenaza de validez interna y de constructo:
el generador siguió emitiendo GET durante una hora, pero después del TTL muchos
fueron rechazados en autenticación y no sometieron a Reservas/Solicitudes a la
misma carga de negocio que antes del minuto 15. Por ello, una tasa 5xx baja no
equivale por sí sola a una prueba completa de disponibilidad sostenida durante
una hora.

La acción correctiva implementó el mecanismo real de refresh sin modificar el
TTL. El smoke demostró 50 refresh correctos, cero fallos de refresh, cero 401 y
continuidad de GET después de t=900 s. La r1 correctiva añadió una ventana
completa con 88.248 GET de negocio y estado de ejecución válido. Esta evidencia
demuestra que la corrección de sesión funciona más allá del TTL, pero no se usa
para fabricar las ocho muestras centrales que exigiría un nuevo IC95.

No se regeneraron, editaron, eliminaron ni ocultaron datos históricos. Los
intentos fallidos siguen separados por número de intento y no se renombran ni se
sobrescriben.

## Afirmaciones defendibles y límites

Se puede defender que:

1. la campaña histórica consta de diez ejecuciones completas de una hora y sus
   conteos son reproducibles desde los raws;
2. su tasa histórica de HTTP 5xx sobre GET cumple matemáticamente el umbral
   preregistrado, con la media e IC95 indicados;
3. la campaña histórica sufrió un defecto de renovación que produjo 668.367
   HTTP 401 y debilitó la carga efectiva sobre el servicio durante gran parte de
   cada hora;
4. la causa fue identificada y corregida mediante refresh real;
5. el smoke y una repetición correctiva completa aportan evidencia real de que
   la sesión renovada mantiene tráfico autenticado más allá de 900 s.

No se debe afirmar que:

1. las diez repeticiones históricas utilizaron refresh;
2. existen diez repeticiones correctivas completas;
3. el smoke o los intentos inválidos forman parte de r2–r9;
4. existe un nuevo IC95 correctivo;
5. la tasa histórica de 5xx demuestra por sí sola una hora completa de carga de
   negocio sostenida o disponibilidad integral del sistema;
6. los HTTP 401 fueron eliminados, irrelevantes o ajenos a los GET intentados.
