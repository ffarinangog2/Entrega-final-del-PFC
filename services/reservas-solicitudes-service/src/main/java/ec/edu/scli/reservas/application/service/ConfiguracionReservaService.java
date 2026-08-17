package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.entity.ConfiguracionReserva;

/** Define el acceso a la configuración activa de reservas. */
public interface ConfiguracionReservaService {

    ConfiguracionReserva obtenerActiva();
}
