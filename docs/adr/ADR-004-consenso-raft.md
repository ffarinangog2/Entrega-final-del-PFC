# ADR-004: Replicación de rangos y consenso Raft para el clúster de reservas

## Estado

Propuesto.

Esta decisión define exclusivamente la estrategia arquitectónica de replicación
del segundo subpaso del Paso 2 de la Entrega 3. No autoriza la creación de
`schema.sql`, la ejecución de SQL ni cambios en fuentes de datos, migraciones,
microservicios, contenedores o automatización.

## Contexto

La arquitectura actual aplica el patrón de base de datos por microservicio.
Auth, Usuarios, Académico-Laboratorios y Reservas-Solicitudes conservan límites
de datos independientes y conexiones propias. La infraestructura de la Entrega
3 incorpora, además, un clúster CockroachDB E3 formado por tres nodos:
`crdb-e3-1`, `crdb-e3-2` y `crdb-e3-3`. Los nodos disponen de almacenamiento
independiente, comparten una red y se descubren mediante la configuración de
unión ya definida.

El ADR-003 adoptó para `reservas-solicitudes-service` una fragmentación
horizontal temporal mediante índices particionados por rango. También separó
la alineación lógica de las particiones de su colocación física y dejó la
replicación para un subpaso posterior. La presente decisión completa únicamente
ese aspecto pendiente: determina cuántas réplicas debe mantener CockroachDB
para cada rango del clúster E3 y explica el mecanismo de consenso que protege
sus cambios.

El comando previsto para materializar la estrategia en un subpaso posterior es:

```sql
ALTER RANGE default CONFIGURE ZONE USING num_replicas = 3;
```

Este ADR documenta el comando, pero no lo ejecuta ni lo incorpora todavía a un
archivo de esquema.

### Problema

Los rangos que contengan las solicitudes, reservas, eventos de historial y
bloqueos de agenda necesitan sobrevivir a la indisponibilidad de un nodo sin
perder consistencia. La estrategia debe ser coherente con un clúster de tres
nodos y con las particiones temporales definidas por el ADR-003, sin introducir
dependencias ni cambios de persistencia en los demás microservicios.

También debe evitarse una interpretación incorrecta del alcance. La
configuración del rango `default` es una política del clúster, no una propiedad
de una tabla o de una aplicación. Por tanto, para que la replicación definida
en este ADR aplique exclusivamente al microservicio principal, el clúster E3
debe alojar únicamente los datos de `reservas-solicitudes-service` en el
alcance actual de la Entrega 3.

## Alternativas consideradas

### Mantener una sola réplica

Una réplica reduce el consumo de almacenamiento y el tráfico entre nodos, pero
convierte al nodo que la contiene en un punto único de fallo. La pérdida o
indisponibilidad de ese nodo deja el rango sin una mayoría capaz de operar. No
cumple el objetivo de tolerar la caída de un nodo.

### Configurar dos réplicas

Dos réplicas permiten conservar una copia adicional, pero Raft necesita una
mayoría para confirmar cambios. En un grupo de dos, la mayoría es de dos; si
falla cualquiera de las réplicas, la restante no puede confirmar nuevas
escrituras. El coste adicional no proporciona la disponibilidad esperada ante
un fallo individual.

### Configurar tres réplicas

Tres réplicas forman un grupo con mayoría de dos. El clúster puede mantener el
quórum cuando una réplica o el nodo que la aloja queda temporalmente
indisponible. Esta opción aprovecha la topología prevista de tres nodos y
representa el mínimo número impar que tolera un fallo individual.

### Configurar cinco o más réplicas

Un número mayor puede tolerar más fallos, pero el clúster actual solo dispone
de tres nodos. No aporta diversidad efectiva de nodos dentro de la topología
definida y aumenta almacenamiento, tráfico de replicación y trabajo de
consenso. Requeriría ampliar primero la infraestructura y revisar sus
restricciones de colocación.

## Decisión

Se define una política predeterminada de tres réplicas para los rangos del
clúster CockroachDB E3:

```sql
ALTER RANGE default CONFIGURE ZONE USING num_replicas = 3;
```

La política se aplicará en el futuro durante la inicialización autorizada del
clúster. En este subpaso solo queda registrada como decisión arquitectónica.
No se crea `schema.sql` ni se ejecuta el comando.

El clúster E3 se considera dedicado, dentro del alcance actual, a la base de
datos principal de `reservas-solicitudes-service`. No se trasladan a él las
bases de Auth, Usuarios o Académico-Laboratorios. Si en una evolución futura el
clúster alojara datos de otros dominios, el efecto global de `RANGE default`
deberá reevaluarse o sustituirse por políticas con un alcance más específico.

### Relación entre replicación y consenso Raft

CockroachDB divide los datos en rangos. Cada rango constituye un grupo de
replicación independiente y sus réplicas participan en Raft. Una de ellas
mantiene el liderazgo del grupo y coordina las propuestas de cambio. Una
escritura se confirma cuando alcanza el quórum del grupo; con tres réplicas, el
quórum es de dos.

Raft ordena y acuerda las modificaciones del rango para que las réplicas no
acepten historias incompatibles. Si el líder deja de estar disponible y
permanecen dos réplicas comunicadas, el grupo puede elegir un nuevo líder y
continuar. La política tolera un fallo individual, pero no garantiza
disponibilidad de escritura cuando dos de las tres réplicas están
indisponibles o separadas entre sí.

`num_replicas = 3` establece el número de réplicas; no sustituye la
inicialización del clúster ni define por sí solo restricciones geográficas o de
localidad. CockroachDB debe poder distribuir las réplicas entre los tres nodos
disponibles. Cualquier garantía adicional sobre regiones, localidades o
restricciones de colocación requerirá una decisión posterior sustentada en la
topología definitiva.

## Justificación técnica

Tres réplicas equilibran disponibilidad y coste para la infraestructura
existente. Constituyen el mínimo grupo Raft capaz de conservar una mayoría tras
un fallo: dos réplicas disponibles de un total de tres. Una o dos réplicas no
ofrecen esa propiedad, mientras que cinco exigirían una topología mayor y
añadirían un coste injustificado para este alcance.

La decisión opera en la capa de almacenamiento distribuido. No cambia modelos
JPA, repositorios, servicios, controladores ni contratos REST. Las aplicaciones
continúan tratando a CockroachDB mediante su interfaz SQL; la replicación y el
consenso son responsabilidades internas del clúster.

La política es compatible con el ADR-003 porque las particiones temporales se
materializarán sobre índices y, en CockroachDB, sus datos se almacenarán en
rangos. La fragmentación determina la organización lógica y favorece la poda
por fecha; la replicación determina cuántas copias de cada rango existen; Raft
mantiene el acuerdo entre esas copias. Son decisiones complementarias, no
alternativas.

## Impacto sobre CockroachDB

- Los rangos sujetos a la política predeterminada mantendrán tres réplicas.
- Cada grupo de réplicas usará Raft para ordenar y confirmar modificaciones.
- Una escritura necesitará el acuerdo de una mayoría de dos réplicas.
- CockroachDB asumirá tráfico de replicación, almacenamiento adicional y
  mantenimiento de las réplicas.
- La configuración no crea particiones, tablas, bases de datos ni reglas de
  localidad.
- La aplicación efectiva de la política queda pendiente del subpaso que
  autorice el esquema y la inicialización del clúster.

## Impacto sobre el clúster de 3 nodos

La estrategia coincide con los tres nodos ya previstos para E3. En condiciones
normales, el asignador de CockroachDB puede distribuir las tres réplicas de un
rango entre nodos distintos, evitando que una única máquina concentre todas
las copias. Con dos réplicas disponibles se conserva el quórum; con una sola no
se pueden confirmar nuevas escrituras.

La replicación no elimina todos los riesgos. Una partición de red puede dejar
operativa únicamente a la fracción que conserve la mayoría, y la pérdida de dos
nodos interrumpe la disponibilidad del rango. Tampoco reemplaza copias de
seguridad, procedimientos de recuperación ni observabilidad, aspectos que
quedan fuera de este subpaso.

## Compatibilidad con la arquitectura actual de microservicios

La decisión no modifica ningún microservicio. Auth, Usuarios y
Académico-Laboratorios mantienen sus bases y despliegues actuales porque son
propietarios de dominios diferentes y no necesitan conocer el mecanismo Raft
del clúster principal.

La replicación se limita al microservicio principal porque el clúster E3 se
reserva, en el alcance vigente, para los datos de
`reservas-solicitudes-service`. Las referencias UUID hacia otros dominios
continúan siendo externas y no se convierten en claves foráneas distribuidas.
No se duplican datos de otros servicios ni se introducen transacciones entre
bases de datos.

## Consecuencias

### Consecuencias positivas

- El clúster puede conservar quórum ante la indisponibilidad de una réplica.
- Los cambios de cada rango se acuerdan mediante Raft antes de confirmarse.
- La política aprovecha los tres nodos previstos sin exigir infraestructura
  adicional.
- Las particiones temporales del ADR-003 reciben protección por replicación sin
  alterar su diseño lógico.
- Los contratos y la implementación de los microservicios permanecen
  desacoplados de la mecánica de consenso.

### Consecuencias negativas

- Cada rango consume aproximadamente tres copias de sus datos, además de la
  sobrecarga interna correspondiente.
- Las escrituras generan comunicación entre réplicas y dependen de una mayoría.
- La pérdida o aislamiento de dos réplicas impide conservar el quórum.
- La política sobre `RANGE default` alcanza todos los datos presentes en el
  clúster E3; mantener el alcance exclusivo requiere que el clúster continúe
  dedicado al microservicio principal.
- La estrategia no define copias de seguridad, localidades, monitorización ni
  recuperación ante desastres.
