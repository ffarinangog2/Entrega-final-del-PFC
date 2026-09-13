package ec.edu.scli.reservas.domain.state.reserva;

import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.Reserva;

import java.time.LocalDateTime;
import java.util.Map;

abstract class AbstractReservaState implements ReservaState {
    private final Map<AccionReserva, EstadoReserva> transiciones;

    protected AbstractReservaState(Map<AccionReserva, EstadoReserva> transiciones) {
        this.transiciones = transiciones;
    }

    @Override
    public EstadoReserva iniciar(Reserva reserva, LocalDateTime ahora) {
        return transicionar(AccionReserva.INICIAR);
    }

    @Override
    public EstadoReserva finalizar() {
        return transicionar(AccionReserva.FINALIZAR);
    }

    @Override
    public EstadoReserva cancelar() {
        return transicionar(AccionReserva.CANCELAR);
    }

    @Override
    public EstadoReserva marcarNoAsistida(Reserva reserva, LocalDateTime ahora) {
        return transicionar(AccionReserva.MARCAR_NO_ASISTIDA);
    }

    protected final EstadoReserva transicionar(AccionReserva accion) {
        EstadoReserva destino = transiciones.get(accion);
        if (destino == null) {
            throw new IllegalStateException(mensajeInvalido(accion));
        }
        return destino;
    }

    private String mensajeInvalido(AccionReserva accion) {
        return switch (accion) {
            case INICIAR -> "La reserva solamente puede iniciar cuando está programada";
            case FINALIZAR -> "La reserva solamente puede finalizar cuando está en curso";
            case CANCELAR -> "La reserva solamente puede cancelarse cuando está programada";
            case MARCAR_NO_ASISTIDA -> "La reserva solamente puede marcarse como no asistida cuando está programada";
        };
    }
}
