package ec.edu.scli.reservas.domain.model;

import java.util.Set;
import java.util.UUID;

public record ActorAutenticado(UUID perfilId, Set<String> authorities) {
    public boolean tiene(String authority) { return authorities.contains(authority); }
}
