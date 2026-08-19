package ec.edu.uteq.scli.mobile.features.reservas.data

import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservaDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ReservaMappersTest {
    @Test
    fun `mapea todos los campos de reserva del gateway al dominio`() {
        val dto = ReservaDto(
            id = "reserva-1",
            solicitudId = "solicitud-1",
            laboratorioId = "laboratorio-1",
            responsableId = "responsable-1",
            fechaReserva = "2026-08-20",
            horaInicio = "08:00:00",
            horaFin = "10:00:00",
            estado = "PROGRAMADA",
            codigoReserva = "RES-001",
            creadaEn = "2026-08-18T10:00:00Z",
            actualizadaEn = "2026-08-18T10:00:00Z",
            version = 0,
        )

        val reserva = dto.toDomain()

        assertEquals(dto.id, reserva.id)
        assertEquals(dto.solicitudId, reserva.solicitudId)
        assertEquals(dto.laboratorioId, reserva.laboratorioId)
        assertEquals(dto.responsableId, reserva.responsableId)
        assertEquals(dto.fechaReserva, reserva.fechaReserva)
        assertEquals(dto.horaInicio, reserva.horaInicio)
        assertEquals(dto.horaFin, reserva.horaFin)
        assertEquals(dto.estado, reserva.estado)
        assertEquals(dto.codigoReserva, reserva.codigoReserva)
        assertEquals(dto.creadaEn, reserva.creadaEn)
        assertEquals(dto.actualizadaEn, reserva.actualizadaEn)
        assertEquals(dto.version, reserva.version)
    }
}
