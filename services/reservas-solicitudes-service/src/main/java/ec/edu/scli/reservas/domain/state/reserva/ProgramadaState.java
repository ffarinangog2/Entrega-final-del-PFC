package ec.edu.scli.reservas.domain.state.reserva;

import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.Reserva;

import java.time.LocalDateTime;
import java.util.Map;

public final class ProgramadaState extends AbstractReservaState {
    public ProgramadaState() {
        super(Map.of(
                AccionReserva.INICIAR, EstadoReserva.EN_CURSO,
                AccionReserva.CANCELAR, EstadoReserva.CANCELADA,
                AccionReserva.MARCAR_NO_ASISTIDA, EstadoReserva.NO_ASISTIDA));
    }

    @Override
    public EstadoReserva iniciar(Reserva reserva, LocalDateTime ahora) {
        LocalDateTime inicio = LocalDateTime.of(reserva.getFechaReserva(), reserva.getHoraInicio());
        if (ahora.isBefore(inicio)) {
            throw new IllegalStateException("La reserva no puede iniciar antes de la fecha y hora programadas");
        }
        return super.iniciar(reserva, ahora);
    }

    @Override
    public EstadoReserva marcarNoAsistida(Reserva reserva, LocalDateTime ahora) {
        LocalDateTime fin = LocalDateTime.of(reserva.getFechaReserva(), reserva.getHoraFin());
        if (ahora.isBefore(fin)) {
            throw new IllegalStateException("La reserva no puede marcarse como no asistida antes de finalizar su franja");
        }
        return super.marcarNoAsistida(reserva, ahora);
    }
}
