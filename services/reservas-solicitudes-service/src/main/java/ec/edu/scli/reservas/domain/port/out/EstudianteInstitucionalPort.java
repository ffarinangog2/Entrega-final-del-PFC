package ec.edu.scli.reservas.domain.port.out;
import java.util.UUID;
public interface EstudianteInstitucionalPort {
    record Contexto(UUID estudianteId, UUID perfilId, UUID carreraId, UUID periodoId, Integer nivel) { }
    Contexto resolverContextoActivo(UUID perfilId);
    Contexto resolverContexto(UUID perfilId, UUID periodoId);
    UUID resolverEstudianteActivo(UUID perfilId);
    UUID resolverCarreraActiva(UUID perfilId);
}
