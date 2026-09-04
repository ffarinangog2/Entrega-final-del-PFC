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
 public Contexto resolverContextoActivo(UUID perfilId){var e=usuarios.obtenerEstudiantePorPerfil(perfilId);
  if(e==null||!e.activo()||!perfilId.equals(e.perfilId())||e.carreraId()==null||e.periodoId()==null||e.nivel()==null)
   throw new ResourceNotFoundException("El estudiante no tiene contexto académico vigente");
  return new Contexto(e.estudianteId(),e.perfilId(),e.carreraId(),e.periodoId(),e.nivel());}
 public Contexto resolverContexto(UUID perfilId,UUID periodoId){var e=usuarios.obtenerContextoEstudiante(perfilId,periodoId);
  if(e==null||!e.activo()||!perfilId.equals(e.perfilId()))throw new ResourceNotFoundException("No existe contexto académico para el ciclo seleccionado");
  return new Contexto(e.estudianteId(),e.perfilId(),e.carreraId(),e.periodoId(),e.nivel());}
 public UUID resolverEstudianteActivo(UUID perfilId){var e=usuarios.obtenerEstudiantePorPerfil(perfilId);
  if(e==null||!e.activo()||!perfilId.equals(e.perfilId()))throw new ResourceNotFoundException("No existe estudiante activo para el perfil autenticado");
  return e.estudianteId();}
 public UUID resolverCarreraActiva(UUID perfilId){var e=usuarios.obtenerEstudiantePorPerfil(perfilId);
  if(e==null||!e.activo()||!perfilId.equals(e.perfilId())||e.carreraId()==null)throw new ResourceNotFoundException("No existe una carrera activa para el estudiante autenticado");
  return e.carreraId();}
}
