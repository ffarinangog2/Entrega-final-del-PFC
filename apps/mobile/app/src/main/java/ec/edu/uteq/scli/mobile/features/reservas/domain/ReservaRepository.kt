package ec.edu.uteq.scli.mobile.features.reservas.domain

import ec.edu.uteq.scli.mobile.common.network.NetworkResult

interface ReservaRepository {
    suspend fun listar(pagina: Int = 0, tamanio: Int = 20): NetworkResult<Pagina<Reserva>>
    suspend fun obtener(id: String): NetworkResult<Reserva>
    suspend fun crearSolicitud(
        solicitud: NuevaSolicitudReserva,
        idempotencyKey: String,
    ): NetworkResult<SolicitudReserva>
    suspend fun actualizarSolicitud(id: String, solicitud: ActualizacionSolicitudReserva): NetworkResult<SolicitudReserva>
    suspend fun cancelarSolicitud(id: String, comentario: String): NetworkResult<SolicitudReserva>
    suspend fun cancelarReserva(id: String, motivo: String): NetworkResult<Reserva>
    suspend fun consultarDisponibilidad(
        laboratorioId: String,
        fecha: String,
        horaInicio: String,
        horaFin: String,
    ): NetworkResult<Disponibilidad>
}

data class NuevaSolicitudReserva(
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

data class ActualizacionSolicitudReserva(
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
