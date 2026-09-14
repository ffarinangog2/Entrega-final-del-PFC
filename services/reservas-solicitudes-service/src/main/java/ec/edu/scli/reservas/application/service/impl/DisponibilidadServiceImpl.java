package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.presentation.dto.response.DisponibilidadResponse;
import ec.edu.scli.reservas.repository.BloqueoAgendaRepository;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.strategy.disponibilidad.ConsultaDisponibilidad;
import ec.edu.scli.reservas.domain.strategy.disponibilidad.DisponibilidadStrategy;
import ec.edu.scli.reservas.domain.strategy.disponibilidad.EstadoLaboratorio;
import ec.edu.scli.reservas.application.service.DisponibilidadService;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacion;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Implementa la consulta de disponibilidad de laboratorios. */
@Service
public class DisponibilidadServiceImpl implements DisponibilidadService {

    private final ReservaRepositoryPort reservaRepository;
    private final BloqueoAgendaRepository bloqueoAgendaRepository;
    private final AcademicoLaboratoriosClient academicoLaboratoriosClient;
    private final DisponibilidadStrategy disponibilidadStrategy;
    private final PlanificacionJpaRepository planificaciones;

    public DisponibilidadServiceImpl(
            ReservaRepositoryPort reservaRepository,
            BloqueoAgendaRepository bloqueoAgendaRepository,
            AcademicoLaboratoriosClient academicoLaboratoriosClient,
            DisponibilidadStrategy disponibilidadStrategy,
            PlanificacionJpaRepository planificaciones) {
        this.reservaRepository = reservaRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.academicoLaboratoriosClient = academicoLaboratoriosClient;
        this.disponibilidadStrategy = disponibilidadStrategy;
        this.planificaciones = planificaciones;
    }

    @Override
    public DisponibilidadResponse consultar(
            UUID laboratorioId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) {
        ConsultaDisponibilidad consulta =
                new ConsultaDisponibilidad(laboratorioId, fecha, horaInicio, horaFin);
        if (planificaciones.existsByLaboratorioIdAndDiaSemanaAndEstadoAndHoraInicioLessThanAndHoraFinGreaterThan(
                laboratorioId, dia(fecha), EstadoPlanificacion.CONFIRMADA, horaFin, horaInicio)) {
            return respuesta(laboratorioId, fecha, horaInicio, horaFin, false,
                    "La franja esta ocupada por planificacion academica confirmada");
        }
        var resultado = disponibilidadStrategy.evaluar(
                consulta,
                LocalDate.now(),
                () -> obtenerEstadoLaboratorio(laboratorioId),
                () -> reservaRepository.contarConflictosActivos(
                        laboratorioId, fecha, horaInicio, horaFin),
                () -> bloqueoAgendaRepository.contarBloqueosActivosConflictivos(
                        laboratorioId, fecha, horaInicio, horaFin));
        return respuesta(laboratorioId, fecha, horaInicio, horaFin,
                resultado.disponible(), resultado.motivo());
    }

    private String dia(LocalDate fecha) {
        return switch (fecha.getDayOfWeek()) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }

    private EstadoLaboratorio obtenerEstadoLaboratorio(UUID laboratorioId) {
        LaboratorioExternoResponse laboratorio =
                academicoLaboratoriosClient.obtenerLaboratorio(laboratorioId);
        return laboratorio == null ? null
                : new EstadoLaboratorio(
                        laboratorio.existe(), laboratorio.activo(), laboratorio.estado());
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
