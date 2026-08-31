package ec.edu.uteq.scli.mobile.features.qr.data

data class LaboratorioDetalleDto(
    val laboratorio: LaboratorioDto?,
    val piso: PisoDto?,
    val bloque: UbicacionDto?,
    val campus: UbicacionDto?,
    val equipos: List<EquipoDto> = emptyList(),
)

data class LaboratorioDto(
    val id: String?,
    val codigo: String?,
    val nombre: String?,
    val capacidad: Int?,
    val descripcion: String?,
    val estado: String?,
    val activo: Boolean?,
)

data class UbicacionDto(
    val id: String?,
    val codigo: String?,
    val nombre: String?,
)

data class PisoDto(
    val id: String?,
    val numero: Int?,
    val descripcion: String?,
)

data class EquipoDto(
    val id: String?,
    val codigoInventario: String?,
    val numeroSerie: String?,
    val marca: String?,
    val modelo: String?,
    val estado: String?,
    val activo: Boolean?,
)

data class LaboratorioDetalle(
    val laboratorio: Laboratorio,
    val piso: Ubicacion?,
    val bloque: Ubicacion?,
    val campus: Ubicacion?,
    val equipos: List<Equipo>,
)

data class Laboratorio(
    val id: String,
    val codigo: String,
    val nombre: String,
    val capacidad: Int?,
    val descripcion: String?,
    val estado: String?,
    val activo: Boolean?,
)

data class Ubicacion(
    val codigo: String?,
    val nombre: String?,
)

data class Equipo(
    val id: String,
    val codigoInventario: String?,
    val numeroSerie: String?,
    val marca: String?,
    val modelo: String?,
    val estado: String?,
    val activo: Boolean?,
)

internal fun LaboratorioDetalleDto.toDomain(): LaboratorioDetalle = LaboratorioDetalle(
    laboratorio = requireNotNull(laboratorio).let {
        Laboratorio(
            id = requireNotNull(it.id),
            codigo = it.codigo.orEmpty(),
            nombre = it.nombre.orEmpty(),
            capacidad = it.capacidad,
            descripcion = it.descripcion,
            estado = it.estado,
            activo = it.activo,
        )
    },
    piso = piso?.let { Ubicacion(codigo = it.numero?.toString(), nombre = it.descripcion ?: it.numero?.let { numero -> "Piso $numero" }) },
    bloque = bloque?.toDomain(),
    campus = campus?.toDomain(),
    equipos = equipos.mapNotNull { equipo ->
        equipo.id?.let {
            Equipo(it, equipo.codigoInventario, equipo.numeroSerie, equipo.marca, equipo.modelo, equipo.estado, equipo.activo)
        }
    },
)

private fun UbicacionDto.toDomain() = Ubicacion(codigo, nombre)
