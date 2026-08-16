# ADR-003: Fragmentación horizontal y colocalización temporal de reservas

## Estado

Propuesto.

La decisión describe el diseño del primer subpaso del Paso 2. No autoriza ni
incluye cambios de esquema, migraciones, configuración de zonas, replicación o
código de aplicación.

## Contexto

`reservas-solicitudes-service` es propietario de las tablas
`solicitudes_reserva`, `reservas`, `historial_solicitudes` y
`bloqueos_agenda`. El esquema vigente se crea mediante Flyway y conserva claves
primarias UUID simples.

Las relaciones persistentes dentro del límite del microservicio son:

- una solicitud puede producir una reserva y cada reserva pertenece a una sola
  solicitud;
- una solicitud tiene múltiples eventos de historial;
- un bloqueo de agenda no tiene una clave foránea hacia las otras tablas, pero
  participa en las mismas consultas operativas mediante laboratorio, fecha y
  franja horaria.

Los identificadores de usuarios, docentes, responsables, materias, periodos y
laboratorios son referencias externas por UUID. No existen claves foráneas
entre bases de datos ni se plantea crearlas.

Las consultas de disponibilidad, calendario y detección de conflictos acotan
los datos por `laboratorio_id` y por rangos temporales. El historial se consulta
por `solicitud_id` y se ordena por `fecha_hora`. En el esquema actual no existe
una columna llamada `fecha_solicitud`; la fecha de creación de una solicitud se
registra en `creada_en`, mientras que la fecha operativa solicitada se registra
en `fecha_reserva`.

CockroachDB implementa `PARTITION BY RANGE` sobre índices. Por ello, una
estrategia que cambiara directamente las claves primarias para anteponer una
fecha afectaría las claves foráneas, las restricciones de unicidad y el mapeo
JPA basado en identificadores UUID. Este ADR debe preservar esos contratos.

### Problema

Se necesita diseñar una fragmentación horizontal temporal que:

- favorezca la poda de rangos en las consultas operativas;
- evite concentrar indefinidamente datos históricos y futuros en un único
  intervalo lógico;
- permita alinear datos relacionados del mismo periodo;
- no mezcle datos pertenecientes a otros microservicios;
- no obligue, en esta etapa, a cambiar entidades, claves, Flyway o despliegue.

La colocalización debe entenderse en dos niveles. Alinear particiones y límites
temporales hace posible una colocación coherente, pero no garantiza por sí sola
la ubicación física de los rangos. Esa garantía requerirá políticas de
localidad o de zona y decisiones de replicación que quedan fuera de este
subpaso.

## Alternativas consideradas

### Mantener únicamente la distribución automática por UUID

CockroachDB distribuiría los rangos automáticamente y las escrituras UUID
tendrían una dispersión razonable. Sin embargo, no existiría una estrategia
explícita de fragmentación temporal ni una base para administrar periodos,
retención o colocación por fecha.

### Fragmentar por hash del identificador

Distribuye uniformemente las escrituras y reduce puntos calientes. No mejora la
poda de consultas por fecha y rompe la alineación natural entre agenda,
solicitudes y reservas de un mismo periodo.

### Fragmentar por laboratorio

Se aproxima a las consultas de disponibilidad, pero la cantidad y actividad de
laboratorios no son uniformes. También requiere mantener particiones cuando se
crean laboratorios y aumenta el riesgo de concentrar la carga de los
laboratorios más usados.

### Fragmentar por estado

Los estados cambian durante el ciclo de vida. Una transición movería entradas
entre particiones y produciría escritura adicional, sin beneficiar las
consultas históricas por fecha.

### Fragmentar por rangos temporales

Coincide con los filtros existentes y permite administrar periodos cerrados.
Requiere mantener límites futuros y conservar particiones de cobertura para no
rechazar datos fuera del calendario previsto.

## Decisión

Se adopta una estrategia de fragmentación horizontal mediante
`PARTITION BY RANGE` sobre índices temporales de CockroachDB, sin sustituir las
claves primarias UUID existentes.

Los intervalos serán trimestrales, con límites de calendario semiabiertos:
inicio incluido y fin excluido. Cada ciclo de implementación deberá mantener
una partición anterior de cobertura, las particiones trimestrales operativas y
una partición futura de cobertura. Los límites concretos se definirán en el
subpaso que produzca el esquema ejecutable; este ADR no fija un año artificial
ni genera DDL.

La clave de partición diseñada para cada tabla es:

| Tabla | Columna temporal | Estrategia |
|---|---|---|
| `solicitudes_reserva` | `fecha_reserva` | Rangos trimestrales por fecha de uso solicitada. |
| `reservas` | `fecha_reserva` | Los mismos límites y nombres lógicos que `solicitudes_reserva`. |
| `historial_solicitudes` | `fecha_hora` | Rangos trimestrales por instante real del evento. |
| `bloqueos_agenda` | `fecha` | Los mismos límites de calendario que solicitudes y reservas. |

La fecha de creación `solicitudes_reserva.creada_en` no será la clave principal
de fragmentación. Aunque representa la fecha lógica de solicitud, las
operaciones críticas consultan la fecha reservada. Se conservará como dato de
auditoría y podrá respaldar un índice no particionado si una medición posterior
lo justifica.

Los índices temporales deberán anteponer la columna de partición y continuar
con las columnas usadas por las consultas:

- fecha, laboratorio y horario para solicitudes, reservas y bloqueos;
- fecha del evento y solicitud para el historial.

La definición exacta de índices, particiones y particiones de cobertura se
realizará posteriormente en `schema.sql` o en el mecanismo de esquema que el
plan autorice. Antes de aplicarla se verificará con `EXPLAIN` que las consultas
usen los índices particionados y que exista poda de particiones.

### Diseño de colocalización

Se alinearán los nombres lógicos y los límites trimestrales de
`solicitudes_reserva`, `reservas` y `bloqueos_agenda`. Así, los datos que
participan en una consulta de agenda para una misma fecha pertenecerán al mismo
intervalo lógico.

`historial_solicitudes` conservará su partición por `fecha_hora`. Sus eventos
normalmente estarán próximos a la creación y tramitación de la solicitud, pero
una transición tardía puede caer en un trimestre distinto. No se duplicará
`fecha_reserva` en el historial solo para forzar colocalización, porque eso
introduciría redundancia y una nueva regla de consistencia.

La colocalización se limita a estas cuatro tablas del mismo microservicio. No
se intentará colocalizar ni crear relaciones físicas con tablas de Auth,
Usuarios o Académico-Laboratorios.

No se asignan particiones a nodos, regiones o zonas en esta decisión. Tampoco
se definen factores de replicación. Esas políticas deberán apoyarse en la
topología definitiva del clúster y pertenecen a subpasos posteriores.

## Justificación técnica

Los rangos trimestrales equilibran la granularidad de poda con el coste de
administración. Son más precisos que rangos anuales para calendario y
disponibilidad, pero evitan la proliferación de particiones mensuales en un
dominio cuyo volumen todavía no ha sido medido.

Usar `fecha_reserva` en solicitudes y reservas mantiene alineados los dos lados
de la relación 1:1 y responde a las consultas reales. Usar `fecha` en bloqueos
permite combinar la agenda con los mismos límites. El historial usa
`fecha_hora` porque es su única fecha propia y representa correctamente el
momento de cada transición.

Conservar las claves primarias UUID evita cambiar el contrato de persistencia,
las asociaciones JPA y las claves foráneas existentes. La partición de índices
secundarios ofrece un camino incremental que puede validarse antes de cualquier
cambio físico más invasivo.

## Impacto sobre CockroachDB

- Se diseñan índices particionados por rango, no tablas nuevas ni bases
  separadas.
- CockroachDB continuará distribuyendo y replicando rangos según su
  configuración vigente hasta que exista una decisión posterior de
  colocación física.
- Las particiones de cobertura evitarán errores para fechas anteriores o
  posteriores a los trimestres declarados.
- La creación y mantenimiento de índices tendrá coste de almacenamiento y de
  escritura.
- La efectividad deberá comprobarse con planes de ejecución y distribución de
  datos antes de promover el diseño.

## Impacto sobre microservicios

No cambia contratos REST, DTO, entidades, repositorios, servicios ni
controladores. Auth, Usuarios y Académico-Laboratorios permanecen fuera de la
decisión. Sus UUID continúan tratándose como referencias externas sin claves
foráneas.

La aplicación de esta decisión será responsabilidad exclusiva del esquema de
`reservas-solicitudes-service` en un subpaso posterior.

## Compatibilidad con la arquitectura existente

La decisión respeta la propiedad de datos por microservicio, Flyway como fuente
actual del esquema, las claves UUID, las relaciones locales y los accesos JPA.
También es compatible con la separación actual de bases de datos y no exige
consultas distribuidas entre microservicios.

La compatibilidad física con el futuro clúster E3 deberá validarse cuando estén
disponibles su topología y `schema.sql`. Este ADR no presupone esos artefactos.

## Consecuencias

### Consecuencias positivas

- Las consultas por rangos de fecha podrán beneficiarse de poda de particiones.
- Solicitudes, reservas y bloqueos compartirán una organización temporal
  coherente para las consultas de agenda.
- Los periodos históricos podrán administrarse de manera explícita.
- Se conserva el modelo Java y el contrato de datos actual.
- El diseño no crea acoplamiento con otros microservicios.

### Consecuencias negativas

- Será necesario crear anticipadamente nuevos rangos trimestrales o mantener
  adecuadamente las particiones de cobertura.
- Los índices particionados consumirán almacenamiento y aumentarán el coste de
  escritura.
- La fragmentación del historial no garantiza que una transición tardía quede
  en el mismo intervalo que la fecha reservada.
- La alineación temporal no equivale a colocalización física hasta definir
  políticas de localidad o zona.
- La estrategia deberá revisarse si las mediciones muestran poco volumen o una
  distribución temporal muy desigual.
