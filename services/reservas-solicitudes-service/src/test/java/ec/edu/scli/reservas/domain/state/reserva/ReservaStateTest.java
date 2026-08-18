package ec.edu.scli.reservas.domain.state.reserva;

import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.Reserva;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaStateTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 17, 12, 0);

    @Test
    void programadaPuedeIniciarCuandoLlegoSuHorario() {
        Reserva reserva = reservaProgramada(LocalTime.of(11, 0), LocalTime.of(13, 0));

        assertEquals(EstadoReserva.EN_CURSO,
                ReservaStates.desde(EstadoReserva.PROGRAMADA).iniciar(reserva, AHORA));
    }

    @Test
    void enCursoPuedeFinalizar() {
        assertEquals(EstadoReserva.FINALIZADA,
                ReservaStates.desde(EstadoReserva.EN_CURSO).finalizar());
    }

    @Test
    void programadaPuedeCancelarse() {
        assertEquals(EstadoReserva.CANCELADA,
                ReservaStates.desde(EstadoReserva.PROGRAMADA).cancelar());
    }

    @Test
    void programadaPuedeMarcarseNoAsistidaAlTerminarLaFranja() {
        Reserva reserva = reservaProgramada(LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertEquals(EstadoReserva.NO_ASISTIDA,
                ReservaStates.desde(EstadoReserva.PROGRAMADA).marcarNoAsistida(reserva, AHORA));
    }

    @Test
    void programadaNoPuedeFinalizarDirectamente() {
        assertThrows(IllegalStateException.class,
                () -> ReservaStates.desde(EstadoReserva.PROGRAMADA).finalizar());
    }

    @Test
    void enCursoNoPuedeCancelarse() {
        assertThrows(IllegalStateException.class,
                () -> ReservaStates.desde(EstadoReserva.EN_CURSO).cancelar());
    }

    private Reserva reservaProgramada(LocalTime inicio, LocalTime fin) {
        Reserva reserva = new Reserva();
        reserva.setFechaReserva(AHORA.toLocalDate());
        reserva.setHoraInicio(inicio);
        reserva.setHoraFin(fin);
        return reserva;
    }
}
