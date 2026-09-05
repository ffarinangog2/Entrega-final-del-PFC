package ec.edu.scli.reservas.infrastructure.client;

import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.domain.model.ContextoInstitucional;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ContextoInstitucionalAdapter implements ContextoInstitucionalPort {
    private final UsuariosClient usuariosClient;

    public ContextoInstitucionalAdapter(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    @Override
    public ContextoInstitucional obtenerPorPerfilId(UUID perfilId) {
        var response = usuariosClient.obtenerContextoInstitucional(perfilId);
        if (response == null) return null;
        var administrador = response.administrador();
        return new ContextoInstitucional(response.existe(), response.activo(),
                administrador != null && administrador.esAdministrador(),
                administrador != null && administrador.activo(),
                administrador != null && administrador.administradorPisoOperativo(),
                administrador == null ? null : administrador.pisoId(),
                response.adscripciones() == null ? java.util.List.of() : response.adscripciones().stream()
                        .filter(a -> a.activo() && "CARRERA".equalsIgnoreCase(a.tipoAmbito()))
                        .map(ec.edu.scli.reservas.client.dto.ContextoInstitucionalExternoResponse.Adscripcion::ambitoId)
                        .toList());
    }
}
