package ec.edu.scli.reservas.domain.strategy.disponibilidad;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DisponibilidadSinConflictosStrategyTest {
    private static final LocalDate HOY = LocalDate.of(2026, 8, 17);
    private final DisponibilidadStrategy strategy = new DisponibilidadSinConflictosStrategy();

    @Test
    void aceptaLaboratorioActivoSinReservasNiBloqueosConflictivos() {
        ResultadoDisponibilidad resultado = strategy.evaluar(
                consulta(LocalTime.of(8, 0), LocalTime.of(10, 0)), HOY,
                () -> new EstadoLaboratorio(true, true, "DISPONIBLE"),
                () -> 0,
                () -> 0);

        assertTrue(resultado.disponible());
        assertNull(resultado.motivo());
    }

    @Test
    void rechazaFranjaConReservaConflictivaSinConsultarBloqueos() {
        AtomicBoolean consultoBloqueos = new AtomicBoolean(false);

        ResultadoDisponibilidad resultado = strategy.evaluar(
                consulta(LocalTime.of(8, 0), LocalTime.of(10, 0)), HOY,
                () -> new EstadoLaboratorio(true, true, "ACTIVO"),
                () -> 1,
                () -> {
                    consultoBloqueos.set(true);
                    return 0;
                });

        assertFalse(resultado.disponible());
        assertEquals("Existe una reserva que cruza el horario solicitado", resultado.motivo());
        assertFalse(consultoBloqueos.get());
    }

    @Test
    void rechazaLimiteConHorasIgualesAntesDeConsultarFuentesExternas() {
        AtomicBoolean consultoLaboratorio = new AtomicBoolean(false);

        assertThrows(IllegalArgumentException.class, () -> strategy.evaluar(
                consulta(LocalTime.of(10, 0), LocalTime.of(10, 0)), HOY,
                () -> {
                    consultoLaboratorio.set(true);
                    return new EstadoLaboratorio(true, true, "DISPONIBLE");
                },
                () -> 0,
                () -> 0));
        assertFalse(consultoLaboratorio.get());
    }

    @Test
    void rechazaBloqueoDeAgendaCuandoNoHayConflictoDeReserva() {
        ResultadoDisponibilidad resultado = strategy.evaluar(
                consulta(LocalTime.of(8, 0), LocalTime.of(10, 0)), HOY,
                () -> new EstadoLaboratorio(true, true, "DISPONIBLE"),
                () -> 0,
                () -> 1);

        assertFalse(resultado.disponible());
        assertEquals(
                "El laboratorio tiene un bloqueo de agenda en el horario solicitado",
                resultado.motivo());
    }

    private ConsultaDisponibilidad consulta(LocalTime inicio, LocalTime fin) {
        return new ConsultaDisponibilidad(UUID.randomUUID(), HOY, inicio, fin);
    }
}
