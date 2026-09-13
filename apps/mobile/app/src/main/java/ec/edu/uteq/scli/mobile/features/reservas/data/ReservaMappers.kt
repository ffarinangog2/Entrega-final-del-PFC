package ec.edu.uteq.scli.mobile.features.reservas.data

import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ActualizarSolicitudReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CrearSolicitudReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.DisponibilidadDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PaginaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.SolicitudReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Disponibilidad
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Pagina
import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.HistorialSolicitud
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.HistorialDto

internal fun ReservaDto.toDomain() = Reserva(
    id, solicitudId, laboratorioId, responsableId, fechaReserva, horaInicio, horaFin,
    estado, codigoReserva, creadaEn, actualizadaEn, version,
)

internal fun SolicitudReservaDto.toDomain() = SolicitudReserva(
    id, solicitanteId, docenteId, laboratorioId, materiaId, periodoLectivoId,
    fechaReserva, horaInicio, horaFin, numeroParticipantes, motivo, observacion,
    estado, reservaId, creadaEn, actualizadaEn, version, propuestaFecha, propuestaHoraInicio,
    propuestaHoraFin, propuestaLaboratorioId, propuestaObservacion,
)

internal fun HistorialDto.toDomain() = HistorialSolicitud(
    id, estadoAnterior, estadoNuevo, usuarioId, comentario, creadoEn,
)

internal fun DisponibilidadDto.toDomain() = Disponibilidad(
    laboratorioId, fecha, horaInicio, horaFin, disponible, motivo,
)

internal fun <T, R> PaginaDto<T>.toDomain(mapper: (T) -> R) = Pagina(
    contenido.map(mapper), pagina, tamanio, totalElementos, totalPaginas, primera, ultima,
)

internal fun NuevaSolicitudReserva.toDto() = CrearSolicitudReservaDto(
    solicitanteId, docenteId, laboratorioId, materiaId, periodoLectivoId, fechaReserva,
    horaInicio, horaFin, numeroParticipantes, motivo, observacion,
)

internal fun ActualizacionSolicitudReserva.toDto() = ActualizarSolicitudReservaDto(
    docenteId, laboratorioId, materiaId, periodoLectivoId, fechaReserva, horaInicio,
    horaFin, numeroParticipantes, motivo, observacion,
)
