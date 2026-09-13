package ec.edu.scli.reservas.security;

import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SpringSecurityActorActualAdapter implements ActorActualPort {
    @Override
    public ActorAutenticado obtener() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AccessDeniedException("No existe un actor JWT autenticado");
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(item -> item.getAuthority()).collect(Collectors.toUnmodifiableSet());
        return new ActorAutenticado(principal.perfilId(), authorities);
    }
}
