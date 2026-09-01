package ec.edu.scli.reservas.infrastructure.client;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.domain.port.out.EstudianteInstitucionalPort;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
public class EstudianteInstitucionalAdapter implements EstudianteInstitucionalPort {
 private final UsuariosClient usuarios;
 public EstudianteInstitucionalAdapter(UsuariosClient usuarios){this.usuarios=usuarios;}
 public UUID resolverEstudianteActivo(UUID perfilId){var e=usuarios.obtenerEstudiantePorPerfil(perfilId);
  if(e==null||!e.activo()||!perfilId.equals(e.perfilId()))throw new ResourceNotFoundException("No existe estudiante activo para el perfil autenticado");
  return e.estudianteId();}
}
