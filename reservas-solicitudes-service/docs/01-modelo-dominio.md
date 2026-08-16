# Modelo de dominio de reservas-solicitudes-service

## 1. Objetivo del dominio

El microservicio `reservas-solicitudes-service` administra el ciclo de vida de las solicitudes de reserva de laboratorios, la reserva que eventualmente se genera al aprobar una solicitud, la trazabilidad de sus cambios de estado, los bloqueos de agenda y las reglas generales de configuración de reservas.

El modelo consolida conceptos que en la base monolítica están distribuidos entre solicitudes de asignación, solicitudes de coordinación, detalles, asignaciones, reservas especiales y horarios. Su propósito es ofrecer un dominio único y coherente, sin reproducir la estructura ni el acoplamiento del monolito.

Este documento define únicamente el modelo conceptual del dominio. No define entidades Java, repositorios, DTO, servicios, controladores, SQL ni configuración.

## 2. Correspondencia con el modelo monolítico

La correspondencia es conceptual: una fila del monolito no necesariamente equivale de forma directa a una fila del nuevo modelo. La consolidación debe realizarse según el significado funcional de los datos.

| Tabla monolítica | Nueva entidad principal | Tratamiento en el nuevo dominio |
|---|---|---|
| `solicitudasignacion` | `SolicitudReserva` | Contiene `id_docente`, `id_horario`, `materia`, `tipo_solicitud`, `estado`, `estado_redireccion`, `observaciones_admin`, `fecha_solicitud` e `id_admin_piso`. Aporta principalmente docente, referencia al horario, materia, tipo y estado de la solicitud, observaciones administrativas y fecha de creación de la petición. No contiene directamente laboratorio, período lectivo ni número de participantes. Tampoco permite obtener de manera uniforme un solicitante identificable como usuario. |
| `solicitudcoordinacion` | `SolicitudReserva`, `HistorialSolicitud` | La cabecera de la solicitud se unifica en `SolicitudReserva`. Los cambios o decisiones relevantes que puedan reconstruirse se representan como eventos en `HistorialSolicitud`. |
| `detallesolicitudcoordinacion` | `SolicitudReserva` | Los detalles necesarios para definir una reserva se integran en la solicitud consolidada. La fragmentación cabecera-detalle del monolito no se conserva como entidad separada. |
| `asignacion_laboratorio` | `SolicitudReserva`, `Reserva` | Contiene `id_laboratorio`, `id_horario`, `id_docente`, `materia`, `id_materia`, `fecha_asignacion`, `id_envio`, `id_solicitud`, `id_admin_piso`, `id_reserva_especial`, `justificacion_admin` e `id_periodo`. Aporta principalmente laboratorio, materia, período, docente, fecha y la relación con la solicitud. Una asignación efectiva se transforma en `Reserva`, mientras sus referencias complementan los datos consolidados de `SolicitudReserva`. |
| `reservaespecial` | `Reserva` o `BloqueoAgenda` | Contiene `unidad_solicitante`, `motivo`, `fecha_inicio`, `fecha_fin`, `id_laboratorio`, `prioridad`, `observaciones`, `publicado` y `titulo`. Sus fechas, laboratorio, motivo y observaciones se conservan según su significado: si representa uso confirmado se transforma en `Reserva`; si representa indisponibilidad sin una solicitud reservable se transforma en `BloqueoAgenda`. `unidad_solicitante` no equivale necesariamente a un usuario solicitante identificable. |
| `horario` | `SolicitudReserva`, `Reserva` o `BloqueoAgenda` | Contiene `dia_semana`, `hora_inicio`, `hora_fin` y `jornada`. Aporta el rango horario y su clasificación semanal, pero no una fecha concreta. La fecha del nuevo modelo debe obtenerse de otras tablas o del contexto de la solicitud, asignación o reserva especial. |
| `horarioenviado` | `SolicitudReserva`, `HistorialSolicitud` | La franja enviada se incorpora a la solicitud; la evidencia de envío o transición se conserva como historial solo cuando tenga valor de auditoría. No se mantiene como entidad independiente. |

`ConfiguracionReserva` no corresponde directamente a una única tabla de la lista. Es un concepto propio del nuevo dominio que centraliza reglas operativas que antes podían estar implícitas, dispersas o implementadas en lógica de aplicación.

Los campos de `SolicitudReserva` no proceden en su totalidad de `solicitudasignacion`. La entidad consolida datos distribuidos entre las tablas monolíticas relacionadas y añade campos requeridos por las reglas actuales del microservicio. En particular, `numeroParticipantes` no aparece en estas tablas y `solicitanteId` no cuenta con una fuente uniforme que identifique a un usuario.

## 3. Entidades del dominio

### 3.1. SolicitudReserva

Representa la petición realizada para utilizar un laboratorio en una fecha y franja horaria determinadas. Es la entidad central del dominio y mantiene el estado actual de la solicitud. También contiene los datos necesarios para evaluar disponibilidad, aplicar reglas y tomar una decisión.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador interno de la solicitud. |
| `solicitanteId` | `UUID` | Nueva referencia externa obligatoria a la persona o usuario que presenta la solicitud; no existe de forma uniforme en las tablas monolíticas relacionadas. |
| `docenteId` | `UUID` | Referencia externa al docente vinculado con la actividad. |
| `laboratorioId` | `UUID` | Referencia externa al laboratorio solicitado. |
| `materiaId` | `UUID` | Referencia externa a la materia asociada. |
| `periodoLectivoId` | `UUID` | Referencia externa al período lectivo. |
| `fechaReserva` | `LocalDate` | Fecha solicitada para la reserva. |
| `horaInicio` | `LocalTime` | Hora de inicio solicitada. |
| `horaFin` | `LocalTime` | Hora de finalización solicitada. |
| `numeroParticipantes` | `Integer` | Nuevo dato requerido por el microservicio para validar la capacidad del laboratorio; no aparece en las tablas monolíticas relacionadas. |
| `motivo` | `String` | Razón o propósito principal de la solicitud. |
| `observacion` | `String` | Información complementaria de la solicitud o su evaluación. |
| `estado` | `EstadoSolicitud` | Estado vigente dentro del ciclo de vida de la solicitud. |
| `creadaEn` | `Instant` | Instante de creación de la solicitud. |
| `actualizadaEn` | `Instant` | Instante de la última actualización. |
| `version` | `Long` | Versión para control de concurrencia. |

`EstadoSolicitud` es un tipo del dominio. Sus valores concretos se definirán al diseñar el ciclo de vida en un paso posterior; este documento no fija todavía una enumeración técnica.

### 3.2. Reserva

Representa la confirmación de uso de un laboratorio. Puede existir únicamente como resultado o materialización de una solicitud dentro de este microservicio, y mantiene su propio estado operativo y código identificador.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador interno de la reserva. |
| `solicitudId` | `UUID` | Referencia interna a la solicitud que originó la reserva. |
| `laboratorioId` | `UUID` | Referencia externa al laboratorio reservado. |
| `responsableId` | `UUID` | Referencia externa a la persona responsable de la reserva. |
| `fechaReserva` | `LocalDate` | Fecha confirmada de uso. |
| `horaInicio` | `LocalTime` | Hora de inicio confirmada. |
| `horaFin` | `LocalTime` | Hora de finalización confirmada. |
| `estado` | `EstadoReserva` | Estado operativo vigente de la reserva. |
| `codigoReserva` | `String` | Código de negocio utilizado para identificar la reserva. |
| `creadaEn` | `Instant` | Instante de creación de la reserva. |
| `actualizadaEn` | `Instant` | Instante de la última actualización. |
| `version` | `Long` | Versión para control de concurrencia. |

`EstadoReserva` es un tipo del dominio. Sus valores concretos se definirán en un paso posterior, junto con las transiciones permitidas.

### 3.3. HistorialSolicitud

Registra de forma cronológica cada transición relevante del estado de una solicitud. Proporciona trazabilidad sobre el estado anterior, el nuevo estado, el actor externo que realizó la acción y el contexto de la decisión.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador interno del registro histórico. |
| `solicitudId` | `UUID` | Referencia interna a la solicitud afectada. |
| `estadoAnterior` | `EstadoSolicitud` | Estado de la solicitud antes de la transición. |
| `estadoNuevo` | `EstadoSolicitud` | Estado de la solicitud después de la transición. |
| `usuarioAccionId` | `UUID` | Referencia externa al usuario que ejecutó o registró la acción. |
| `comentario` | `String` | Explicación, justificación u observación de la transición. |
| `fechaHora` | `Instant` | Instante en que ocurrió la transición. |

### 3.4. BloqueoAgenda

Representa una franja en la que un laboratorio no puede recibir reservas. Permite modelar mantenimiento, cierres institucionales, actividades internas u otras indisponibilidades que no nacen de una solicitud de reserva.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador interno del bloqueo. |
| `laboratorioId` | `UUID` | Referencia externa al laboratorio bloqueado. |
| `fecha` | `LocalDate` | Fecha a la que se aplica el bloqueo. |
| `horaInicio` | `LocalTime` | Inicio de la franja bloqueada. |
| `horaFin` | `LocalTime` | Fin de la franja bloqueada. |
| `motivo` | `String` | Razón de la indisponibilidad. |
| `creadoPor` | `UUID` | Referencia externa al usuario que creó el bloqueo. |
| `activo` | `Boolean` | Indica si el bloqueo continúa vigente. |
| `creadoEn` | `Instant` | Instante de creación del bloqueo. |
| `version` | `Long` | Versión para control de concurrencia. |

### 3.5. ConfiguracionReserva

Agrupa las reglas globales utilizadas para validar solicitudes y reservas. Su vigencia se expresa mediante `activo`, lo que permite conservar configuraciones anteriores y determinar cuál debe aplicarse.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador interno de la configuración. |
| `anticipacionMinimaHoras` | `Integer` | Horas mínimas de anticipación requeridas para solicitar una reserva. |
| `anticipacionMaximaDias` | `Integer` | Días máximos de anticipación permitidos. |
| `duracionMinimaMinutos` | `Integer` | Duración mínima permitida para una reserva. |
| `duracionMaximaMinutos` | `Integer` | Duración máxima permitida para una reserva. |
| `permiteFinesSemana` | `Boolean` | Indica si se admiten reservas durante fines de semana. |
| `activo` | `Boolean` | Indica si la configuración está vigente. |
| `creadaEn` | `Instant` | Instante de creación de la configuración. |
| `actualizadaEn` | `Instant` | Instante de la última actualización. |
| `version` | `Long` | Versión para control de concurrencia. |

## 4. Relaciones y límites de persistencia

- `SolicitudReserva` se relaciona de **uno a muchos** con `HistorialSolicitud`: una solicitud puede acumular varios registros históricos y cada registro pertenece a una sola solicitud.
- `SolicitudReserva` se relaciona de **uno a cero o uno** con `Reserva`: una solicitud puede no haber generado una reserva, o puede generar como máximo una. Cada reserva pertenece a una única solicitud.
- `Reserva.solicitudId` y `HistorialSolicitud.solicitudId` pueden y deben representar relaciones locales, porque las entidades relacionadas pertenecen a la misma base de datos y al mismo límite transaccional de `reservas-solicitudes-service`.
- `BloqueoAgenda` y `ConfiguracionReserva` no dependen estructuralmente de una solicitud concreta. Participan en la evaluación de disponibilidad y reglas, pero no requieren una relación persistente con `SolicitudReserva`.

Los campos `solicitanteId`, `docenteId`, `laboratorioId`, `materiaId`, `periodoLectivoId`, `responsableId`, `usuarioAccionId` y `creadoPor` son referencias `UUID` externas. Identifican recursos cuyo dato maestro pertenece a otros microservicios o contextos. Por ello se almacenan como valores, **sin claves foráneas** hacia bases de datos externas y sin relaciones ORM locales. La validez o existencia de esas referencias se resolverá mediante los contratos de integración correspondientes, no mediante integridad referencial entre bases.

Dentro de esas referencias, `solicitanteId` es un campo nuevo y obligatorio del modelo distribuido: ni `solicitudasignacion` ni las demás tablas relacionadas proporcionan de manera uniforme un identificador de usuario que pueda trasladarse directamente. Su valor deberá resolverse durante la captura o migración de la solicitud mediante el contexto de identidad correspondiente.

## 5. Diagrama textual

```text
                         +-----------------------+
                         | ConfiguracionReserva  |
                         | reglas vigentes       |
                         +-----------+-----------+
                                     |
                                     | condiciona validaciones
                                     v
+------------------+       1   +-----+------------+   0..1   +----------------+
| HistorialSolicitud| * <------+ SolicitudReserva +---------->+ Reserva        |
| solicitudId local |          | entidad central  |           | solicitudId    |
+------------------+          +---------+---------+           | local          |
                                       |                     +----------------+
                                       | consulta disponibilidad
                                       v
                              +------------------+
                              | BloqueoAgenda    |
                              | por laboratorio  |
                              | y franja         |
                              +------------------+

Referencias externas UUID (sin FK):
  solicitanteId, docenteId, laboratorioId, materiaId, periodoLectivoId,
  responsableId, usuarioAccionId y creadoPor.
```

Las líneas hacia `ConfiguracionReserva` y `BloqueoAgenda` expresan colaboración conceptual durante las validaciones, no asociaciones persistentes obligatorias.

## 6. Conservación, transformación y descarte de información

### 6.1. Información que se conserva

Se conserva la información con valor para gestionar y auditar una reserva:

- Las identidades disponibles de docentes, laboratorios, materias y períodos lectivos, convertidas a referencias externas `UUID`; otras identidades se incorporan según los contratos del modelo distribuido.
- Las fechas disponibles en solicitudes, asignaciones o reservas especiales y el rango horario aportado por `horario`; `horario` por sí solo no contiene una fecha concreta.
- El número de participantes y el propósito de la solicitud, cuando estén disponibles.
- Las observaciones y comentarios relevantes para evaluar o justificar decisiones.
- El estado vigente de solicitudes y reservas.
- La relación entre una solicitud y la ocupación confirmada que produce.
- Las transiciones de estado que tengan valor de auditoría, junto con actor, comentario y fecha.
- Las indisponibilidades de laboratorio que afecten la agenda.
- Las marcas temporales útiles para creación y actualización.

### 6.2. Información que se transforma

- Los identificadores propios del monolito se normalizan a `UUID`; los identificadores de otros dominios pasan a ser referencias externas sin clave foránea.
- Las distintas variantes de solicitud (`solicitudasignacion`, `solicitudcoordinacion` y su detalle), junto con datos de `asignacion_laboratorio`, `horario` y otras tablas relacionadas, se consolidan en `SolicitudReserva`. Ninguna tabla aislada aporta todos sus campos.
- `solicitanteId` se incorpora como una nueva referencia `UUID` externa obligatoria, porque el monolito no identifica de manera uniforme al solicitante como usuario.
- `numeroParticipantes` se incorpora como un dato nuevo requerido para validar la capacidad del laboratorio, ya que no aparece en las tablas monolíticas relacionadas.
- Las asignaciones efectivas y reservas especiales que representan uso confirmado se convierten en `Reserva`.
- Las reservas especiales u horarios que solo expresan indisponibilidad se convierten en `BloqueoAgenda`.
- Las horas de `horario` se normalizan como `LocalTime`; la `LocalDate` se obtiene de la solicitud, asignación, reserva especial o contexto correspondiente, nunca de `horario` por sí solo. Los momentos de auditoría se expresan como `Instant`.
- Los estados heterogéneos del monolito se traducen a los tipos de dominio `EstadoSolicitud` y `EstadoReserva`.
- La evidencia histórica distribuida entre solicitudes, envíos y decisiones se convierte, cuando sea posible, en una secuencia de `HistorialSolicitud`.
- Las reglas operativas implícitas o dispersas se hacen explícitas mediante `ConfiguracionReserva`.
- Se incorporan campos `version` en las entidades mutables que requieren control de concurrencia.

### 6.3. Información que se descarta

- La duplicación de datos maestros pertenecientes a usuarios, docentes, laboratorios, materias o períodos lectivos; el nuevo dominio conserva únicamente sus identificadores externos.
- Las estructuras técnicas del monolito usadas solo para enlazar cabeceras, detalles, asignaciones u horarios, cuando su significado ya queda representado por las nuevas entidades.
- Los campos obsoletos, redundantes, derivados o sin valor funcional para solicitar, aprobar, reservar, bloquear o auditar.
- La distinción estructural entre los diferentes tipos heredados de solicitud cuando no modifica el comportamiento del nuevo dominio.
- Registros técnicos de envío de horario que no representen una transición, una franja solicitada o evidencia útil de auditoría.
- Relaciones por clave foránea con tablas que quedan fuera del microservicio.

El descarte debe entenderse como una decisión del modelo de destino. Durante una futura migración de datos deberá verificarse la semántica real y la calidad de cada campo del monolito antes de aplicar estas reglas; esa migración no forma parte de este paso.
