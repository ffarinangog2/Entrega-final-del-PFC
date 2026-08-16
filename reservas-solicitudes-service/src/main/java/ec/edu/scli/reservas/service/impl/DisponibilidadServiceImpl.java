package ec.edu.scli.reservas.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.dto.response.DisponibilidadResponse;
import ec.edu.scli.reservas.repository.BloqueoAgendaRepository;
import ec.edu.scli.reservas.repository.ReservaRepository;
import ec.edu.scli.reservas.service.DisponibilidadService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Implementa la consulta de disponibilidad de laboratorios. */
@Service
public class DisponibilidadServiceImpl implements DisponibilidadService {

    private final ReservaRepository reservaRepository;
    private final BloqueoAgendaRepository bloqueoAgendaRepository;
    private final AcademicoLaboratoriosClient academicoLaboratoriosClient;

    public DisponibilidadServiceImpl(
            ReservaRepository reservaRepository,
            BloqueoAgendaRepository bloqueoAgendaRepository,
            AcademicoLaboratoriosClient academicoLaboratoriosClient) {
        this.reservaRepository = reservaRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.academicoLaboratoriosClient = academicoLaboratoriosClient;
    }

    @Override
    public DisponibilidadResponse consultar(
            UUID laboratorioId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) {
        validarArgumentos(laboratorioId, fecha, horaInicio, horaFin);

        LaboratorioExternoResponse laboratorio =
                academicoLaboratoriosClient.obtenerLaboratorio(laboratorioId);

        if (laboratorio == null || !laboratorio.existe()) {
            throw new IllegalArgumentException("El laboratorio indicado no existe");
        }
        if (!laboratorio.activo()) {
            throw new IllegalArgumentException("El laboratorio indicado no está activo");
        }
        if (laboratorio.estado() != null && !esEstadoDisponible(laboratorio.estado())) {
            return respuesta(laboratorioId, fecha, horaInicio, horaFin, false,
                    "El laboratorio no se encuentra disponible");
        }

        long conflictosReserva = reservaRepository.contarConflictosActivos(
                laboratorioId, fecha, horaInicio, horaFin);
        if (conflictosReserva > 0) {
            return respuesta(laboratorioId, fecha, horaInicio, horaFin, false,
                    "Existe una reserva que cruza el horario solicitado");
        }

        long bloqueos = bloqueoAgendaRepository.contarBloqueosActivosConflictivos(
                laboratorioId, fecha, horaInicio, horaFin);
        if (bloqueos > 0) {
            return respuesta(laboratorioId, fecha, horaInicio, horaFin, false,
                    "El laboratorio tiene un bloqueo de agenda en el horario solicitado");
        }

        return respuesta(laboratorioId, fecha, horaInicio, horaFin, true, null);
    }

    private void validarArgumentos(
            UUID laboratorioId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) {
        if (laboratorioId == null || fecha == null || horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException(
                    "El laboratorio, la fecha y las horas de inicio y fin son obligatorios");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede estar en el pasado");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser mayor que la hora de inicio");
        }
    }

    private boolean esEstadoDisponible(String estado) {
        return "DISPONIBLE".equalsIgnoreCase(estado) || "ACTIVO".equalsIgnoreCase(estado);
    }

    private DisponibilidadResponse respuesta(
            UUID laboratorioId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            boolean disponible,
            String motivo) {
        return new DisponibilidadResponse(
                laboratorioId, fecha, horaInicio, horaFin, disponible, motivo);
    }
}
