package ec.edu.uteq.scli.mobile.features.reservas.data.remote

data class PaginaDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val primera: Boolean,
    val ultima: Boolean,
)

data class ReservaDto(
    val id: String,
    val solicitudId: String,
    val laboratorioId: String,
    val responsableId: String,
    val fechaReserva: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: String,
    val codigoReserva: String,
    val creadaEn: String,
    val actualizadaEn: String,
    val version: Long,
)

data class SolicitudReservaDto(
    val id: String,
    val solicitanteId: String,
    val docenteId: String,
    val laboratorioId: String,
    val materiaId: String,
    val periodoLectivoId: String,
    val fechaReserva: String,
    val horaInicio: String,
    val horaFin: String,
    val numeroParticipantes: Int,
    val motivo: String,
    val observacion: String?,
    val estado: String,
    val reservaId: String?,
    val creadaEn: String,
    val actualizadaEn: String,
    val version: Long,
    val propuestaFecha: String? = null,
    val propuestaHoraInicio: String? = null,
    val propuestaHoraFin: String? = null,
    val propuestaLaboratorioId: String? = null,
    val propuestaObservacion: String? = null,
)

data class DisponibilidadDto(
    val laboratorioId: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val disponible: Boolean,
    val motivo: String?,
)

data class CrearSolicitudReservaDto(
    val solicitanteId: String,
    val docenteId: String,
    val laboratorioId: String,
    val materiaId: String,
    val periodoLectivoId: String,
    val fechaReserva: String,
    val horaInicio: String,
    val horaFin: String,
    val numeroParticipantes: Int,
    val motivo: String,
    val observacion: String?,
)

data class ActualizarSolicitudReservaDto(
    val docenteId: String,
    val laboratorioId: String,
    val materiaId: String,
    val periodoLectivoId: String,
    val fechaReserva: String,
    val horaInicio: String,
    val horaFin: String,
    val numeroParticipantes: Int,
    val motivo: String,
    val observacion: String?,
)

data class CancelarSolicitudDto(val comentario: String)
data class CancelarReservaDto(val motivo: String)
data class ComentarioDto(val comentario: String? = null)
data class AprobarSolicitudDto(val responsableId: String, val comentario: String? = null)
data class PropuestaDto(
    val laboratorioId: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val observacion: String? = null,
)
data class HistorialDto(
    val id: String,
    val estadoAnterior: String?,
    val estadoNuevo: String,
    val usuarioId: String,
    val comentario: String?,
    val creadoEn: String,
)
