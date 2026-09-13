package ec.edu.uteq.scli.mobile.features.reservas.data

import ec.edu.uteq.scli.mobile.features.reservas.data.remote.DisponibilidadDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.HistorialDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PaginaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.SolicitudReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
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

    @Test
    fun `mapea solicitud de reserva incluyendo propuesta alternativa`() {
        val dto = SolicitudReservaDto(
            id = "solicitud-1",
            solicitanteId = "solicitante-1",
            docenteId = "docente-1",
            laboratorioId = "laboratorio-1",
            materiaId = "materia-1",
            periodoLectivoId = "periodo-1",
            fechaReserva = "2026-08-20",
            horaInicio = "08:00",
            horaFin = "10:00",
            numeroParticipantes = 20,
            motivo = "Clase de laboratorio",
            observacion = "Sin observaciones",
            estado = "PROPUESTA",
            reservaId = null,
            creadaEn = "2026-08-18T10:00:00Z",
            actualizadaEn = "2026-08-18T10:00:00Z",
            version = 2,
            propuestaFecha = "2026-08-21",
            propuestaHoraInicio = "09:00",
            propuestaHoraFin = "11:00",
            propuestaLaboratorioId = "laboratorio-2",
            propuestaObservacion = "Cambio de horario",
        )

        val solicitud = dto.toDomain()

        assertEquals(dto.id, solicitud.id)
        assertEquals(dto.solicitanteId, solicitud.solicitanteId)
        assertEquals(dto.docenteId, solicitud.docenteId)
        assertEquals(dto.laboratorioId, solicitud.laboratorioId)
        assertEquals(dto.materiaId, solicitud.materiaId)
        assertEquals(dto.periodoLectivoId, solicitud.periodoLectivoId)
        assertEquals(dto.fechaReserva, solicitud.fechaReserva)
        assertEquals(dto.horaInicio, solicitud.horaInicio)
        assertEquals(dto.horaFin, solicitud.horaFin)
        assertEquals(dto.numeroParticipantes, solicitud.numeroParticipantes)
        assertEquals(dto.motivo, solicitud.motivo)
        assertEquals(dto.observacion, solicitud.observacion)
        assertEquals(dto.estado, solicitud.estado)
        assertEquals(dto.reservaId, solicitud.reservaId)
        assertEquals(dto.creadaEn, solicitud.creadaEn)
        assertEquals(dto.actualizadaEn, solicitud.actualizadaEn)
        assertEquals(dto.version, solicitud.version)
        assertEquals(dto.propuestaFecha, solicitud.propuestaFecha)
        assertEquals(dto.propuestaHoraInicio, solicitud.propuestaHoraInicio)
        assertEquals(dto.propuestaHoraFin, solicitud.propuestaHoraFin)
        assertEquals(dto.propuestaLaboratorioId, solicitud.propuestaLaboratorioId)
        assertEquals(dto.propuestaObservacion, solicitud.propuestaObservacion)
    }

    @Test
    fun `mapea historial de solicitud al dominio`() {
        val dto = HistorialDto(
            id = "historial-1",
            estadoAnterior = "PENDIENTE",
            estadoNuevo = "APROBADA",
            usuarioId = "usuario-1",
            comentario = "Aprobado por disponibilidad",
            creadoEn = "2026-08-18T12:00:00Z",
        )

        val historial = dto.toDomain()

        assertEquals(dto.id, historial.id)
        assertEquals(dto.estadoAnterior, historial.estadoAnterior)
        assertEquals(dto.estadoNuevo, historial.estadoNuevo)
        assertEquals(dto.usuarioId, historial.usuarioId)
        assertEquals(dto.comentario, historial.comentario)
        assertEquals(dto.creadoEn, historial.creadoEn)
    }

    @Test
    fun `mapea disponibilidad al dominio`() {
        val dto = DisponibilidadDto(
            laboratorioId = "laboratorio-1",
            fecha = "2026-08-20",
            horaInicio = "08:00",
            horaFin = "10:00",
            disponible = false,
            motivo = "Ocupado por otra reserva",
        )

        val disponibilidad = dto.toDomain()

        assertEquals(dto.laboratorioId, disponibilidad.laboratorioId)
        assertEquals(dto.fecha, disponibilidad.fecha)
        assertEquals(dto.horaInicio, disponibilidad.horaInicio)
        assertEquals(dto.horaFin, disponibilidad.horaFin)
        assertEquals(dto.disponible, disponibilidad.disponible)
        assertEquals(dto.motivo, disponibilidad.motivo)
    }

    @Test
    fun `mapea pagina generica aplicando el mapper a cada elemento`() {
        val dto = PaginaDto(
            contenido = listOf("uno", "dos", "tres"),
            pagina = 1,
            tamanio = 3,
            totalElementos = 9,
            totalPaginas = 3,
            primera = false,
            ultima = false,
        )

        val pagina = dto.toDomain { it.uppercase() }

        assertEquals(listOf("UNO", "DOS", "TRES"), pagina.contenido)
        assertEquals(dto.pagina, pagina.pagina)
        assertEquals(dto.tamanio, pagina.tamanio)
        assertEquals(dto.totalElementos, pagina.totalElementos)
        assertEquals(dto.totalPaginas, pagina.totalPaginas)
        assertEquals(dto.primera, pagina.primera)
        assertEquals(dto.ultima, pagina.ultima)
    }

    @Test
    fun `mapea nueva solicitud de dominio a dto de creacion`() {
        val nueva = NuevaSolicitudReserva(
            solicitanteId = "solicitante-1",
            docenteId = "docente-1",
            laboratorioId = "laboratorio-1",
            materiaId = "materia-1",
            periodoLectivoId = "periodo-1",
            fechaReserva = "2026-08-20",
            horaInicio = "08:00",
            horaFin = "10:00",
            numeroParticipantes = 15,
            motivo = "Practica",
            observacion = null,
        )

        val dto = nueva.toDto()

        assertEquals(nueva.solicitanteId, dto.solicitanteId)
        assertEquals(nueva.docenteId, dto.docenteId)
        assertEquals(nueva.laboratorioId, dto.laboratorioId)
        assertEquals(nueva.materiaId, dto.materiaId)
        assertEquals(nueva.periodoLectivoId, dto.periodoLectivoId)
        assertEquals(nueva.fechaReserva, dto.fechaReserva)
        assertEquals(nueva.horaInicio, dto.horaInicio)
        assertEquals(nueva.horaFin, dto.horaFin)
        assertEquals(nueva.numeroParticipantes, dto.numeroParticipantes)
        assertEquals(nueva.motivo, dto.motivo)
        assertEquals(nueva.observacion, dto.observacion)
    }

    @Test
    fun `mapea actualizacion de solicitud de dominio a dto`() {
        val actualizacion = ActualizacionSolicitudReserva(
            docenteId = "docente-2",
            laboratorioId = "laboratorio-2",
            materiaId = "materia-2",
            periodoLectivoId = "periodo-2",
            fechaReserva = "2026-08-22",
            horaInicio = "14:00",
            horaFin = "16:00",
            numeroParticipantes = 30,
            motivo = "Cambio de horario",
            observacion = "Reprogramada",
        )

        val dto = actualizacion.toDto()

        assertEquals(actualizacion.docenteId, dto.docenteId)
        assertEquals(actualizacion.laboratorioId, dto.laboratorioId)
        assertEquals(actualizacion.materiaId, dto.materiaId)
        assertEquals(actualizacion.periodoLectivoId, dto.periodoLectivoId)
        assertEquals(actualizacion.fechaReserva, dto.fechaReserva)
        assertEquals(actualizacion.horaInicio, dto.horaInicio)
        assertEquals(actualizacion.horaFin, dto.horaFin)
        assertEquals(actualizacion.numeroParticipantes, dto.numeroParticipantes)
        assertEquals(actualizacion.motivo, dto.motivo)
        assertEquals(actualizacion.observacion, dto.observacion)
    }
}
