package ec.edu.scli.reservas.domain.state.reserva;

import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.Reserva;

import java.time.LocalDateTime;

public interface ReservaState {
    EstadoReserva iniciar(Reserva reserva, LocalDateTime ahora);
    EstadoReserva finalizar();
    EstadoReserva cancelar();
    EstadoReserva marcarNoAsistida(Reserva reserva, LocalDateTime ahora);
}
