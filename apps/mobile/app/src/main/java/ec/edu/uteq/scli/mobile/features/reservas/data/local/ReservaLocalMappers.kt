package ec.edu.uteq.scli.mobile.features.reservas.data.local

import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva

internal fun Reserva.toEntity() = ReservaEntity(
    id, solicitudId, laboratorioId, responsableId, fechaReserva, horaInicio, horaFin,
    estado, codigoReserva, creadaEn, actualizadaEn, version,
)

internal fun ReservaEntity.toDomain() = Reserva(
    id, solicitudId, laboratorioId, responsableId, fechaReserva, horaInicio, horaFin,
    estado, codigoReserva, creadaEn, actualizadaEn, version,
)
