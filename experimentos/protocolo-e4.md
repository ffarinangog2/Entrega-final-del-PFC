# Protocolo ISO 25010 de Freddy: Reservas/Solicitudes

## Objetivo

Evaluar las cinco características ISO 25010 declaradas por el proyecto. Para
fiabilidad y eficiencia de desempeño se observan las consultas HTTP de
`reservas-solicitudes-service` bajo carga nominal. Este documento define además,
antes de su ejecución, las campañas de seguridad, mantenibilidad y compatibilidad;
no contiene resultados de esas campañas.

## Entorno que debe registrarse

Antes de cada lote se debe conservar:

- fecha y hora UTC, commit y estado del árbol Git;
- host objetivo y despliegue utilizado, sin publicar secretos;
- sistema operativo, CPU, memoria y límites de recursos;
- versiones de Java, Spring Boot, Locust, Prometheus y Grafana;
- número de instancias del servicio y su configuración relevante;
- topología y estado de CockroachDB, junto con la instantánea de datos;
- procesos externos o incidencias capaces de afectar la medición;
- comando exacto de carga y archivos de configuración utilizados.

El ambiente, los datos y la configuración deben permanecer constantes durante las diez
repeticiones equivalentes. Cualquier desviación se registra en `observacion`.

## Escenarios

### Eficiencia nominal

- 50 usuarios concurrentes.
- Duración: 5 minutos por repetición.
- Carga de solo lectura definida en `tests/load/locustfile.py`.
- Criterio: latencia p95 menor que 500 ms.

Comando base, con `LOCUST_HOST` configurado para el ambiente autorizado:

```powershell
python -m locust -f tests/load/locustfile.py --headless --users 50 --spawn-rate 10 --run-time 5m --csv evidencia/eficiencia-rNN --html evidencia/eficiencia-rNN.html
```

### Fiabilidad nominal

- 50 usuarios concurrentes.
- Duración: 1 hora por repetición.
- La misma carga de solo lectura y el mismo ritmo de incorporación de usuarios.
- Criterio de error HTTP: porcentaje de respuestas HTTP 5xx menor que 1 %.
- Repeticiones previstas: 10; repeticiones ejecutadas: 0.
- Estado actual: **NO EJECUTADA / NO CONCLUYENTE**.

```powershell
python -m locust -f tests/load/locustfile.py --headless --users 50 --spawn-rate 10 --run-time 1h --csv evidencia/fiabilidad-rNN --html evidencia/fiabilidad-rNN.html
```

`NN` se reemplaza por la repetición `01` a `10`. No se debe cambiar el host ni
la carga entre repeticiones comparables.

La campaña exige diez ejecuciones independientes de una hora, es decir, al
menos diez horas efectivas de observación continua, además de preparación del
entorno, monitoreo, recolección y validación de evidencia. No se completó dentro
de la ventana disponible de la entrega. Por integridad experimental no se usan
repeticiones parciales ni resultados del escenario de cinco minutos como
sustitutos de la campaña de fiabilidad.

El porcentaje HTTP 5xx mide respuestas fallidas respecto de solicitudes y no
equivale a disponibilidad temporal. El objetivo de disponibilidad de 99,5 %
requiere definir y observar tiempo apto frente a tiempo total durante las diez
ventanas completas. Hasta contar con esa evidencia, tanto la fiabilidad temporal
como la disponibilidad permanecen **NO CONCLUYENTES**; un valor 5xx menor que
1 % por sí solo no demuestra disponibilidad mayor o igual que 99,5 %.

## Repetibilidad y validez

Cada escenario se ejecuta diez veces (`r = 10`) y se registra con una fila por
repetición en `resultados/iso25010.csv`.

- Las repeticiones 1 y 10 se conservan como evidencia, pero se excluyen del análisis.
- Las repeticiones 2 a 9 forman las ocho muestras candidatas.
- Una muestra es válida cuando completa la duración, conserva el ambiente previsto y
  dispone de métricas reales y evidencia trazable.
- Las ejecuciones interrumpidas o alteradas no se eliminan: se marcan como no válidas y
  se explica la causa.
- El código de salida real de Locust se conserva separadamente. Un código distinto de
  cero causado por respuestas HTTP fallidas no invalida una ventana que completó la hora,
  mantuvo el entorno y preservó toda la evidencia; los HTTP 500 siguen siendo resultados.
- Una ventana abortada, con cambio de Git/despliegue o evidencia incompleta se rechaza.
- No se reemplazan datos ausentes por cero ni se estiman mediciones.

El análisis solo produce estadísticas cuando existen exactamente ocho muestras válidas
y completas para el escenario.

## Recolección de métricas

Locust conserva el total de peticiones, sus fallos, percentiles y reporte HTML. Para el
CSV de este protocolo, `failures` registra respuestas HTTP 5xx observadas; otros fallos
de Locust se documentan adicionalmente en `observacion` y en los archivos originales.

Prometheus permite obtener el porcentaje 5xx de la hora completa con:

```promql
100 * sum(increase(http_server_requests_seconds_count{job="reservas-solicitudes-service",status=~"5.."}[1h])) / clamp_min(sum(increase(http_server_requests_seconds_count{job="reservas-solicitudes-service"}[1h])), 1)
```

Para la p95 del escenario de cinco minutos se usan los buckets del alias requerido:

```promql
1000 * histogram_quantile(0.95, sum by (le) (increase(http_request_duration_seconds_bucket{job="reservas-solicitudes-service"}[5m])))
```

El resultado anterior está en milisegundos. Los valores de Prometheus deben tomarse
usando exactamente el intervalo UTC de cada repetición. La p95 de Locust sirve como
contraste; cualquier discrepancia se conserva y explica, no se corrige manualmente.

## Análisis estadístico

Para cada métrica y escenario, con las ocho observaciones válidas `x_i`:

- media: `x̄ = Σx_i / n`, con `n = 8`;
- desviación estándar muestral: `s = sqrt(Σ(x_i - x̄)² / (n - 1))`;
- IC 95 %: `x̄ ± t(0.975, 7) × s / sqrt(8)`, usando `t = 2.364624251`.

La decisión es conservadora: eficiencia cumple cuando el límite superior del IC 95 %
de p95 es menor que 500 ms. El analizador puede decidir únicamente el criterio
acotado de tasa HTTP 5xx cuando el límite superior de su IC 95 % es menor que
1 %; esa decisión no se etiqueta como disponibilidad. El script
`analizar_iso25010.py` aplica estas reglas y no calcula resultados con muestras
incompletas.

## Evidencia que debe conservarse

Por cada repetición se deben guardar:

- CSV y HTML originales de Locust;
- instante UTC de inicio y fin y comando ejecutado;
- exportación o captura de las consultas Prometheus usadas;
- captura del dashboard Grafana correspondiente al intervalo;
- logs del servicio durante la ejecución;
- manifiesto del entorno y estado de CockroachDB;
- fila completa en `resultados/iso25010.csv` y explicación de anomalías.

Los artefactos deben usar nombres con escenario y número de repetición. La plantilla
versionada no constituye evidencia ni resultado.

## Pre-registro E3: seguridad, mantenibilidad y compatibilidad

### Estado, alcance y regla estadística común

Este apartado se fija **antes de ejecutar** las nuevas campañas. En el momento
del pre-registro hay cero observaciones nuevas y el antecedente Android de
45,69 % de cobertura de líneas no se reutilizará como resultado del nuevo HEAD.
El criterio actual de consolidación para el nivel 10 exige las cinco
características medidas, IC95, umbral previo, resultados desfavorables
documentados y trazabilidad hasta evidencia raw. No exige ocho ni diez
repeticiones, tampoco una matriz Android API 26/34. La rúbrica histórica
versionada prescribe diez repeticiones sólo para el experimento Spark.

Cada campaña tendrá `r = 3` ejecuciones medidas sobre el mismo SHA y entorno. Es
el mínimo homogéneo adoptado: permite detectar una ejecución no repetible y, en
la cobertura, estimar variación entre ejecuciones, sin imponer las ocho muestras
de las campañas de carga a mediciones con otra unidad observacional. No habrá
calentamiento ni descartes por posición. Ninguna observación se eliminará después
por conveniencia. Un proceso abortado, cambio de SHA o entorno, o raw incompleto
se conserva como incidencia y hace incompleta la campaña hasta repetir la
ejecución con un nuevo identificador; nunca se sustituye por cero ni se borra.

Todos los comandos se ejecutarán sobre un mismo SHA limpio. El manifiesto de
cada ejecución registrará SHA, fecha UTC, sistema, versiones, comando, código de
salida y hashes SHA-256 de los raw. Los fallos, reintentos y flakiness se
conservarán aunque una repetición posterior resulte favorable. No se modificará
un umbral después de observar resultados.

### Seguridad: decisiones dinámicas de autorización por Gateway

La población es una matriz fija de siete decisiones ejecutadas contra el API
Gateway con las fixtures que ya usa el repositorio: `admin`/`Admin123!` y las
cuentas suministradas mediante `DEMO_DOCENTE_USERNAME`,
`DEMO_DOCENTE_PASSWORD`, `DEMO_ADMIN_PISO_USERNAME` y
`DEMO_ADMIN_PISO_PASSWORD`; los laboratorios son `DEMO-LAB-A` y `DEMO-LAB-B`.
La campaña se declarará inválida si esas fixtures no existen; no se crearán
usuarios ni endpoints para completar la muestra. La matriz comprende: login
administrador permitido (200); login con su contraseña deliberadamente
incorrecta (401); `GET /api/v1/reservas` sin credencial (401); login DOCENTE
permitido (200); creación, consulta y cancelación de su solicitud permitidas
(201/200/200, contadas como una decisión compuesta que sólo acierta si las tres
respuestas coinciden); actuación de ADMINISTRADOR_PISO dentro de su piso
permitida (200); y propuesta en `DEMO-LAB-B`, fuera de su scope, rechazada
(403). Las rutas y expectativas proceden de
`apps/web/e2e/auth.spec.ts`, `apps/web/e2e/reservas-freddy.spec.ts` y
`docs/iso25010/matriz-seguridad-endpoints.csv`.

| Campo | Pre-registro |
|---|---|
| Métrica y unidad | Proporción de decisiones con código HTTP esperado, en porcentaje: `100 * correctas / N`. Se registrarán además falsos permitidos y falsos rechazados como conteos. |
| Población/escenario | Las siete decisiones anteriores, por Gateway, con datos demo reales y estado restaurado antes de cada repetición. |
| Observaciones | Una observación binaria es el resultado correcto/incorrecto de una de las siete assertions de decisión en una ejecución. Las 21 ejecuciones de assertions, siete en cada una de `r = 3` ejecuciones, forman la matriz experimental predefinida. La inferencia queda acotada a esta matriz, fixtures, Gateway y entorno, no a toda posible autorización del sistema. |
| Umbral previo | 100 % de decisiones observadas correctas; cero falsos permitidos, cero falsos rechazados y cero flakiness. |
| Regla de decisión | Cumple sólo si `correctas/21 = 100 %`, no hay flakiness y ambos conteos son cero. El IC95 Wilson se reporta como medida de precisión de la proporción observada, pero su límite inferior no se compara con 100 %, porque eso es matemáticamente imposible con una muestra finita. Cualquier acceso incorrectamente permitido o rechazado se conserva y publica como resultado desfavorable. |
| IC95 | Wilson bilateral 95 % sobre las `N` decisiones observadas: `((p̂ + z²/(2N)) ± z*sqrt(p̂(1-p̂)/N + z²/(4N²))) / (1 + z²/N)`, con `p̂ = correctas/N` y `z = 1.959963985`. Es preferible a Student para resultados binarios y permanece acotado a `[0,1]`. No hay `s`; si todas aciertan, el intervalo Wilson sigue siendo no degenerado y se informa sin alterarlo. |
| Comando real | Desde `apps/web`: `npm run test:e2e -- --project=chromium --grep "Autenticación web|Flujo Freddy integrado" --reporter=json`. Las comprobaciones HTTP directas 401 se ejecutan con `curl.exe` contra `$env:GATEWAY_BASE_URL`; sus cabeceras, cuerpo, código y salida se guardan sin secretos. |
| Entorno y duración | Despliegue integrado con Gateway y fixtures demo; 10--20 min por ejecución, aproximadamente 30--60 min más preparación. Puede ejecutarse localmente con Docker si reproduce el despliegue y las fixtures; la VM integrada es preferible. |
| Raw futuro | `resultados/raw/seguridad_gateway/rep-NN/`, con `manifest.json`, `decisiones.csv`, `playwright.json`, respuestas `login-invalido-*` y `reservas-sin-token-*`, y `servicios.log`. Se conservará una fila por cada una de las siete decisiones, incluida toda discrepancia. |

El reporte Playwright por sí solo no demuestra el 401 del Gateway: ese caso
debe aparecer en `decisiones.csv` y en la respuesta raw de `curl.exe`. Tampoco
se inferirá causalidad a partir de un código observado.

Con `$rawDir` apuntando al directorio de la repetición, los dos controles 401
se capturan exactamente así (la contraseña inválida es la fixture negativa ya
presente en `auth.spec.ts`):

```powershell
curl.exe -sS -D "$rawDir/login-invalido-headers.txt" -o "$rawDir/login-invalido-body.json" -w "%{http_code}" -X POST "$env:GATEWAY_BASE_URL/api/v1/auth/login" -H "Content-Type: application/json" --data '{"username":"admin","password":"contraseña-incorrecta"}' > "$rawDir/login-invalido-status.txt"
curl.exe -sS -D "$rawDir/reservas-sin-token-headers.txt" -o "$rawDir/reservas-sin-token-body.json" -w "%{http_code}" "$env:GATEWAY_BASE_URL/api/v1/reservas" > "$rawDir/reservas-sin-token-status.txt"
```

### Mantenibilidad: cobertura reproducible por componente

La medida principal será cobertura de líneas sobre el mismo SHA. Se conservará
por separado para Auth, Usuarios, Académico, Reservas, Gateway, Web y Android;
queda prohibido promediar componentes, plataformas o denominadores distintos.
Las ramas de Reservas y las ramas, funciones y sentencias de Web se conservan
como puertas secundarias ya configuradas. La complejidad y Checkstyle se
registran como evidencia estática complementaria, pero no se les fabricará un
IC mediante repeticiones de un valor determinista.

| Campo | Pre-registro |
|---|---|
| Métrica y unidad | Por componente: porcentaje de líneas cubiertas `100 * cubiertas / total`. Secundarias: Reservas, ramas; Web, ramas/funciones/sentencias, también en porcentaje. |
| Población/escenario | Todo el código instrumentable incluido por JaCoCo/Vitest en cada uno de los siete componentes, con sus exclusiones versionadas, ejecutando su suite completa sobre el mismo SHA. |
| Observaciones | Para cada componente y métrica, una observación es el porcentaje que produce una ejecución completa del proceso de cobertura. Se realizan `n = 3` ejecuciones limpias por componente. Cada componente constituye un estrato; las líneas no se presentan como muestras independientes. |
| Umbral previo | Líneas: Auth, Usuarios, Académico, Gateway, Web y Android >= 70 %; Reservas >= 80 %. Secundarias: ramas de Reservas >= 48 % y ramas, funciones y sentencias Web >= 70 %. Android 70 % queda predeclarado aquí porque no posee puerta JaCoCo; 45,69 % permanece sólo como antecedente histórico. |
| Regla de decisión | Para cada componente se informan las tres observaciones, media, desviación muestral e IC95. Como mayor cobertura es mejor, el componente cumple cuando las tres ejecuciones terminan correctamente y el límite inferior del IC95 es `>=` a su umbral; sus puertas secundarias, si aplican, deben satisfacer la misma regla. Mantenibilidad cumple sólo si cumplen los siete componentes; todo fallo de suite, flakiness o incumplimiento se conserva y publica individualmente. |
| IC95 | Por componente y métrica: `x̄ ± t(0.975,2)*s/sqrt(3)`, con `t = 4.302652730`, truncado sólo para presentación al rango `[0,100]`; la decisión usa el límite sin truncar. Este IC mide variabilidad entre ejecuciones del proceso de medición, no incertidumbre sobre una población de código. Si `s = 0`, el IC es degenerado `[x̄,x̄]` y se informa honestamente. Se conservan numerador y denominador raw. |
| Comandos reales | En cada `services/<servicio>`: `./mvnw --batch-mode clean verify` (o `mvn --batch-mode clean verify` si no hay wrapper). En `apps/web`: `npm run test:coverage`. En `apps/mobile`: `./gradlew clean testDebugUnitTest jacocoTestReport`. |
| Entorno y duración | Java 17/Maven, Node/npm y JDK 17/Gradle Android; ejecución local posible. 15--30 min por ejecución del lote, aproximadamente 45--90 min más preparación. |
| Raw futuro | `resultados/raw/mantenibilidad/rep-NN/manifest.json`, XML/HTML JaCoCo de cada servicio y Android, `coverage-summary.json`/LCOV de Web, salidas completas y códigos de proceso. |

Los umbrales backend proceden de los `pom.xml` versionados; los de Web, de la
puerta de `.github/workflows/ci-cd.yml`. Los reportes ya soportan como unidad
alternativa los conteos de líneas cubiertas y totales, pero tratarlos como una
muestra binomial supondría independencia entre líneas y una población aleatoria
que este estudio no tiene. Por ello se conserva el IC entre ejecuciones. La
repetición estima estabilidad de la suite y su instrumentación; no convierte
cobertura en una medida causal ni independiza líneas de una misma ejecución.

### Compatibilidad: motores Web

La campaña E3 queda acotada a compatibilidad Web, la matriz ejecutable declarada
en `apps/web/playwright.config.ts`: Chromium, Firefox y WebKit. Este alcance
satisface la medición cuantitativa de la característica sin afirmar
compatibilidad universal. Android permanece como evidencia móvil separada del
proyecto: `minSdk = 26` define configuración, y la ejecución histórica API 29
demuestra sólo ese entorno. La rúbrica actual no exige API 26 ni API 34, por lo
que no se incorporan a esta campaña estadística.

| Campo | Pre-registro |
|---|---|
| Métrica y unidad | Por motor: porcentaje `100 * casos aprobados / N`. Fallido, interrumpido o saltado cuenta como no aprobado. Una suite sin casos elegibles invalida la ejecución. |
| Población/escenario | Lista fija de casos de la suite E2E Web del SHA, ejecutada separadamente en Chromium, Firefox y WebKit. El alcance inferencial se limita a esos casos, versiones de motor y entorno. No se promedian motores. |
| Observaciones | Una observación binaria es el resultado aprobado/no aprobado de un caso en un motor y ejecución. Se realizan `r = 3` ejecuciones completas por motor; `N = 3 * K`, donde `K` es la cantidad de casos elegibles congelada con el SHA antes de la primera ejecución. |
| Umbral previo | Por motor: 100 % de casos ejecutados aprobados; cero fallos, cero casos flaky y cero casos omitidos. |
| Regla de decisión | Un motor cumple sólo si el resultado observado de la suite completa satisface `aprobados/N = 100 %`, sin fallos, casos flaky ni omitidos. El IC95 Wilson se reporta como medida de precisión de la proporción observada, pero su límite inferior no se compara con 100 %, porque eso es matemáticamente imposible con una muestra finita. Compatibilidad cumple sólo si cumplen los tres motores. Toda desviación se documenta como resultado desfavorable. |
| IC95 | Wilson bilateral 95 % por motor: `((p̂ + z²/(2N)) ± z*sqrt(p̂(1-p̂)/N + z²/(4N²))) / (1 + z²/N)`, con `p̂ = aprobados/N` y `z = 1.959963985`. Es apropiado para resultados binarios y queda en `[0,1]`. No hay `s`; con todos los casos aprobados el intervalo continúa siendo no degenerado y se informa tal cual. |
| Comandos reales | En `apps/web`, por separado: `npm run test:e2e -- --project=chromium --reporter=json`, y equivalentes con `firefox` y `webkit`. |
| Entorno y duración | Ejecución local con Node y los tres motores Playwright; aproximadamente 10--20 min por vuelta de los tres motores, 30--60 min totales más preparación. |
| Raw futuro | `resultados/raw/compatibilidad/web/<motor>/rep-NN/{manifest.json,playwright.json,test-results/}`. |

Los reintentos configurados por Playwright forman parte de la ejecución y el raw
conservará cada intento. Un caso que falla inicialmente y pasa al reintentar se
marca flaky y hace incumplir el umbral, aunque su estado final sea aprobado. No
se atribuirá un fallo a navegador, red o aplicación sin evidencia diagnóstica
independiente.

Los IC95 Wilson de seguridad y compatibilidad caracterizan la precisión de las
proporciones en las ejecuciones observadas. El IC95 t de mantenibilidad
caracteriza la variabilidad del proceso de medición entre ejecuciones. Ninguno
de estos intervalos implica universalidad fuera de la matriz, suite,
componentes, versiones y entorno efectivamente ensayados.

## Pre-registro ARBITER S0--S4

### Estado y aislamiento

Este apartado pre-registra el experimento exigido por la guía FUVV, secciones
5.1--5.6. A la fecha existen **0 de 130 corridas ejecutadas**. No contiene
resultados. El producto reserva laboratorios; como el experimento requiere un
equipo concreto, se utiliza un estado experimental separado con `equipmentId`,
franja, agente, estado, versión, `runId` y marcas temporales. No se modifica la
API ni la persistencia productiva.

El subsistema solo se habilita con `EXPERIMENTAL_ARBITER_ENABLED=true`, un valor
válido de `ARBITER` y la clave interna. Sin esas tres condiciones no se inicia.
`ARBITER` ausente no selecciona ninguna estrategia y el flujo productivo sigue
usando su implementación actual (`SERIALIZABLE`, reintentos, idempotencia,
mutex de agenda y validación de disponibilidad). S0 nunca es predeterminado.

### Tratamientos predefinidos

- **S0, escritura directa sin arbitraje:** control negativo exclusivamente
  experimental; no hace atómica la decisión y permite confirmaciones solapadas.
- **S1, bloqueo optimista:** lee versión y confirma mediante compare-and-set;
  el perdedor observa la versión modificada.
- **S2, bloqueo pesimista:** bloquea `equipmentId + inicio + fin` antes de
  comprobar conflictos y decidir.
- **S3, coordinador elegido:** tres nodos lógicos, algoritmo Bully (mayor ID
  vivo), heartbeat/detección en el backend, reloj Lamport y sección serial del
  líder. Lamport/Bully se reconstruyeron para E4 porque el artefacto E1 citado
  por la guía no está presente en HEAD.
- **S4, serializable por quórum:** transacción `SERIALIZABLE` y reintento por
  `SerializationFailure` en CockroachDB real. Su liderazgo pertenece al
  consenso del clúster; no es coordinador de aplicación ni usa Bully.

El almacenamiento SQL es administrado por `reservas-solicitudes-service` y,
solo con el modo experimental habilitado, crea el esquema `scli_experimental`;
los tests usan dobles en memoria. El harness nunca escribe directamente en
CockroachDB. Los equipos pueden
leerse de Académico. `fixtures/equipos-experimentales.json` es una fixture
sintética versionada y nunca representa inventario institucional.

### Matriz oficial

| Escenario | Configuraciones | Repeticiones | Corridas |
|---|---:|---:|---:|
| Esc-1, nominal: 50 usuarios/5 min | productiva | 10 | 10 |
| Esc-2: 50 simultáneas, mismo equipo/franja | S0--S4 | 10 | 50 |
| Esc-3: 200 simultáneas, mismo equipo/franja | S0--S4 | 10 | 50 |
| Esc-4: 200 simultáneas, caída al 50 % | S3 y S4 | 10 | 20 |
| **Total** | | | **130** |

Se conserva toda repetición. Las repeticiones 1 y 10 se excluyen del análisis;
2--9 forman ocho muestras candidatas. Se alternará el orden de estrategias
entre bloques y se mantendrá la semilla registrada. S0+Esc-3 debe exhibir doble
adjudicación; si no ocurre, se invalida el bloque comparativo y se revisan la
barrera y la posible presencia accidental de arbitraje.

### Generación, caída y recuperación

`generador_rafagas.py` usa un `Barrier` para agentes y coordinador, y conserva
`run_id`, SHA, semilla, tiempos de preparación/envío/respuesta y latencia. No se
ejecutan ráfagas en el PC. En Esc-4, S3 desactiva el líder Bully al 50 % y
registra la nueva elección. Para S4, `caida_coordinador.py` exige confirmación y
un comando reversible específico de la VM para detener un nodo Cockroach sin
borrar datos ni volúmenes. Se mide separadamente recuperación del líder de
aplicación S3 y recuperación del servicio respaldado por consenso S4.

### Oráculo y variables respuesta

El oráculo verifica: (1) ausencia de confirmaciones solapadas por equipo; (2)
exactamente un adjudicatario por confirmación; (3) ausencia de adjudicación
sobre equipo en mantenimiento; (4) exactamente un `RELEASED` por `CANCELLED`;
y (5) ningún `ACCESS_GRANTED` sin adjudicación vigente del mismo usuario,
equipo y franja. Sin eventos de cancelación o acceso, esos invariantes se
informan `NOT_OBSERVED`, no `PASS`. El quinto valida la regla experimental, no
una integración con hardware universitario.

La tasa principal es la proporción de franjas con más de un usuario confirmado,
con intervalo binomial Wilson 95 %. Un rechazo es innecesario cuando no existe
confirmación incompatible que lo justifique. Para Jain, `xi` es el número de
adjudicaciones confirmadas de cada agente; se informa además el vector por
equipo. El tiempo de recuperación va desde la marca de fallo hasta la vuelta
funcional y conserva ambos timestamps.

Por métrica se calcularán media, desviación muestral, IC95 y mediana. Las
comparaciones predefinidas son S1--S4 contra S0 dentro del mismo escenario y,
además, S1 vs S2, S2 vs S4 y S3 vs S4. Se usa Mann--Whitney bilateral y A12 con
grupo A igual a la primera estrategia nombrada. A12 representa
`P(A>B)+0.5P(A=B)`. Menor es favorable para dobles, rechazos, latencia y
recuperación; mayor es favorable para Jain. Se usarán diagramas de caja.

### Amenazas pre-registradas

- Ruido del host y competencia: descarte analítico de primera/última repetición,
  registro de entorno y alternancia del orden.
- Sincronía artificial de la barrera: aumenta deliberadamente la contención y
  no representa todas las llegadas reales.
- Orden de tratamientos: se alterna entre bloques.
- Cantidad sintética de equipos/laboratorios: limita generalización al campus.
- Demanda sintética frente a picos reales: limita validez externa.

La evidencia real irá a `resultados/arbiter/raw`, `oracle`, `summary` y
`analysis`. Solo después de ejecutar se generará un `SHA256SUMS` separado, sin
alterar el manifiesto histórico ISO 25010.

### Preparación reproducible (no ejecutar en PC)

En la VM autorizada se fija el SHA desplegado y se exportan, sin registrarlas,
la clave y URL internas del servicio. S4 utiliza el datasource CockroachDB real
configurado en `reservas-solicitudes-service`:

```text
EXPERIMENTAL_ARBITER_ENABLED=true
ARBITER=s4
INTERNAL_API_KEY=<secreto de la VM>
RESERVAS_EXPERIMENTAL_URL=http://<reservas-interno>/api/v1/internal/experimentos/arbiter/adjudicar
```

El plan determinista se materializa con
`python experimentos/planificar_arbiter.py --output <ruta>/plan.json`. Cada
corrida Esc-2/3 se invoca con `generador_rafagas.py --strategy <sN>
--scenario <esc2|esc3> --rep <1..10> --equipment-id <id> --laboratory-id <id>
--starts-at <ISO-8601> --ends-at <ISO-8601>`. Esc-4/S4 añade el comando
reversible y el health interno; exige el literal de confirmación
`CONFIRM_EXPERIMENTAL_NODE_FAILURE`. El oráculo se ejecuta sobre cada manifiesto
y la fixture/catálogo declarado. Ningún comando contiene credenciales en sus
argumentos ni ejecuta las 130 corridas automáticamente.
