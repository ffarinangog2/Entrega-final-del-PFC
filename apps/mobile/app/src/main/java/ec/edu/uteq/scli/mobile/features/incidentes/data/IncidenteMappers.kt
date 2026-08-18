package ec.edu.uteq.scli.mobile.features.incidentes.data

import ec.edu.uteq.scli.mobile.features.incidentes.domain.Incidente
import ec.edu.uteq.scli.mobile.features.incidentes.domain.Prioridad

fun IncidenteEntity.toDomain(): Incidente = Incidente(
    id = id,
    laboratorioEquipo = laboratorioEquipo,
    descripcion = descripcion,
    prioridad = Prioridad.valueOf(prioridad),
    fechaMillis = fechaMillis,
    creadoEnMillis = creadoEnMillis,
)

fun Incidente.toEntity(): IncidenteEntity = IncidenteEntity(
    id = id,
    laboratorioEquipo = laboratorioEquipo,
    descripcion = descripcion,
    prioridad = prioridad.name,
    fechaMillis = fechaMillis,
    creadoEnMillis = creadoEnMillis,
)
