package ec.edu.scli.reservas.domain.strategy.disponibilidad;

import java.time.LocalDate;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class DisponibilidadSinConflictosStrategy implements DisponibilidadStrategy {

    @Override
    public ResultadoDisponibilidad evaluar(
            ConsultaDisponibilidad consulta,
            LocalDate fechaActual,
            Supplier<EstadoLaboratorio> laboratorioSupplier,
            LongSupplier conflictosReserva,
            LongSupplier bloqueosAgenda) {
        validarConsulta(consulta, fechaActual);

        EstadoLaboratorio laboratorio = laboratorioSupplier.get();
        if (laboratorio == null || !laboratorio.existe()) {
            throw new IllegalArgumentException("El laboratorio indicado no existe");
        }
        if (!laboratorio.activo()) {
            throw new IllegalArgumentException("El laboratorio indicado no está activo");
        }
        if (laboratorio.estado() != null && !esEstadoDisponible(laboratorio.estado())) {
            return noDisponible("El laboratorio no se encuentra disponible");
        }
        if (conflictosReserva.getAsLong() > 0) {
            return noDisponible("Existe una reserva que cruza el horario solicitado");
        }
        if (bloqueosAgenda.getAsLong() > 0) {
            return noDisponible("El laboratorio tiene un bloqueo de agenda en el horario solicitado");
        }
        return new ResultadoDisponibilidad(true, null);
    }

    private void validarConsulta(ConsultaDisponibilidad consulta, LocalDate fechaActual) {
        if (consulta.laboratorioId() == null || consulta.fecha() == null
                || consulta.horaInicio() == null || consulta.horaFin() == null) {
            throw new IllegalArgumentException(
                    "El laboratorio, la fecha y las horas de inicio y fin son obligatorios");
        }
        if (consulta.fecha().isBefore(fechaActual)) {
            throw new IllegalArgumentException("La fecha no puede estar en el pasado");
        }
        if (!consulta.horaFin().isAfter(consulta.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser mayor que la hora de inicio");
        }
    }

    private boolean esEstadoDisponible(String estado) {
        return "DISPONIBLE".equalsIgnoreCase(estado) || "ACTIVO".equalsIgnoreCase(estado);
    }

    private ResultadoDisponibilidad noDisponible(String motivo) {
        return new ResultadoDisponibilidad(false, motivo);
    }
}
