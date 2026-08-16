package ec.edu.scli.reservas.service.impl;

import ec.edu.scli.reservas.entity.ConfiguracionReserva;
import ec.edu.scli.reservas.repository.ConfiguracionReservaRepository;
import ec.edu.scli.reservas.service.ConfiguracionReservaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa el acceso a la configuracion activa de reservas. */
@Service
public class ConfiguracionReservaServiceImpl implements ConfiguracionReservaService {

    private final ConfiguracionReservaRepository configuracionReservaRepository;

    public ConfiguracionReservaServiceImpl(ConfiguracionReservaRepository configuracionReservaRepository) {
        this.configuracionReservaRepository = configuracionReservaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionReserva obtenerActiva() {
        return configuracionReservaRepository.findByActivoTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe una configuración de reservas activa"));
    }
}
