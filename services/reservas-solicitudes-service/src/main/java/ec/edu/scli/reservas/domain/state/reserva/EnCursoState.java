package ec.edu.scli.reservas.domain.state.reserva;

import ec.edu.scli.reservas.domain.model.EstadoReserva;

import java.util.Map;

public final class EnCursoState extends AbstractReservaState {
    public EnCursoState() {
        super(Map.of(AccionReserva.FINALIZAR, EstadoReserva.FINALIZADA));
    }
}
