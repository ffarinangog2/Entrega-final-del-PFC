package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.entity.ConfiguracionReserva;
import ec.edu.scli.reservas.repository.ConfiguracionReservaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracionReservaServiceImplTest {
    @Test
    void devuelveConfiguracionActiva() {
        ConfiguracionReservaRepository repository = mock(ConfiguracionReservaRepository.class);
        ConfiguracionReserva configuracion = mock(ConfiguracionReserva.class);
        when(repository.findByActivoTrue()).thenReturn(Optional.of(configuracion));
        assertSame(configuracion, new ConfiguracionReservaServiceImpl(repository).obtenerActiva());
    }

    @Test
    void fallaCuandoNoExisteConfiguracionActiva() {
        ConfiguracionReservaRepository repository = mock(ConfiguracionReservaRepository.class);
        when(repository.findByActivoTrue()).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class,
                () -> new ConfiguracionReservaServiceImpl(repository).obtenerActiva());
    }
}
