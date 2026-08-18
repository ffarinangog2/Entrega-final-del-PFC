package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.strategy.disponibilidad.DisponibilidadSinConflictosStrategy;
import ec.edu.scli.reservas.repository.BloqueoAgendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisponibilidadServiceImplTest {
    private ReservaRepositoryPort reservas;
    private BloqueoAgendaRepository bloqueos;
    private AcademicoLaboratoriosClient cliente;
    private DisponibilidadServiceImpl service;
    private final UUID laboratorioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reservas = mock(ReservaRepositoryPort.class);
        bloqueos = mock(BloqueoAgendaRepository.class);
        cliente = mock(AcademicoLaboratoriosClient.class);
        service = new DisponibilidadServiceImpl(
                reservas, bloqueos, cliente, new DisponibilidadSinConflictosStrategy());
        when(cliente.obtenerLaboratorio(laboratorioId))
                .thenReturn(new LaboratorioExternoResponse(laboratorioId, true, true, "DISPONIBLE", 30));
    }

    @Test
    void informaLaboratorioDisponible() {
        assertTrue(consultar().disponible());
    }

    @Test
    void informaConflictoDeReserva() {
        when(reservas.contarConflictosActivos(any(), any(), any(), any())).thenReturn(1L);
        assertEquals("Existe una reserva que cruza el horario solicitado", consultar().motivo());
        verifyNoInteractions(bloqueos);
    }

    @Test
    void informaBloqueoDeAgenda() {
        when(bloqueos.contarBloqueosActivosConflictivos(any(), any(), any(), any())).thenReturn(1L);
        assertEquals("El laboratorio tiene un bloqueo de agenda en el horario solicitado", consultar().motivo());
    }

    @Test
    void informaEstadoExternoNoDisponible() {
        when(cliente.obtenerLaboratorio(laboratorioId))
                .thenReturn(new LaboratorioExternoResponse(laboratorioId, true, true, "MANTENIMIENTO", 30));
        assertFalse(consultar().disponible());
        verifyNoInteractions(reservas, bloqueos);
    }

    private ec.edu.scli.reservas.presentation.dto.response.DisponibilidadResponse consultar() {
        return service.consultar(laboratorioId, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(10, 0));
    }
}
