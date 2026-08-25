package ec.edu.scli.reservas.infrastructure.client;

import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.client.dto.DocenteExternoResponse;
import ec.edu.scli.reservas.domain.model.DocenteInstitucional;
import ec.edu.scli.reservas.domain.port.out.DocenteInstitucionalPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocenteInstitucionalAdapter implements DocenteInstitucionalPort {
    private final UsuariosClient usuarios;

    public DocenteInstitucionalAdapter(UsuariosClient usuarios) { this.usuarios = usuarios; }

    @Override public DocenteInstitucional obtenerPorPerfilId(UUID perfilId) {
        return map(usuarios.obtenerDocentePorPerfil(perfilId));
    }
    @Override public DocenteInstitucional obtenerPorDocenteId(UUID docenteId) {
        return map(usuarios.obtenerDocentePorId(docenteId));
    }
    private DocenteInstitucional map(DocenteExternoResponse response) {
        return response == null ? null
                : new DocenteInstitucional(response.docenteId(), response.perfilId(), response.activo());
    }
}
