package ec.edu.scli.academico.security;

import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.infrastructure.client.ContextoInstitucionalResponse;
import ec.edu.scli.academico.infrastructure.client.UsuariosContextoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PoliticaAmbitoAcademicoTest {
    private final UsuariosContextoClient contextos = mock(UsuariosContextoClient.class);
    private final PoliticaAmbitoAcademico policy = new PoliticaAmbitoAcademico(
            contextos, mock(LaboratorioService.class), mock(MateriaService.class));
    private final UUID profileId = UUID.randomUUID();

    @BeforeEach void prepare() { }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void adminPisoAResourcePisoAAllowed() {
        UUID pisoA = UUID.randomUUID(); authenticate("ADMINISTRADOR_PISO");
        when(contextos.obtener(profileId)).thenReturn(context(pisoA, List.of()));
        assertDoesNotThrow(() -> policy.validarPiso(pisoA));
    }

    @Test void adminPisoUsaElMismoPisoParaLectura() {
        UUID pisoA = UUID.randomUUID(); authenticate("ADMINISTRADOR_PISO");
        when(contextos.obtener(profileId)).thenReturn(context(pisoA, List.of()));
        assertEquals(pisoA, policy.pisoParaLectura());
    }

    @Test void otrosRolesConservanLecturaGlobalDeLaboratorios() {
        authenticate("COORDINADOR");
        assertNull(policy.pisoParaLectura());
    }

    @Test void adminPisoAResourcePisoBForbidden() {
        authenticate("ADMINISTRADOR_PISO");
        when(contextos.obtener(profileId)).thenReturn(context(UUID.randomUUID(), List.of()));
        assertThrows(AccessDeniedException.class, () -> policy.validarPiso(UUID.randomUUID()));
    }

    @Test void coordinatorCareerAResourceCareerAAllowed() {
        UUID careerA = UUID.randomUUID(); authenticate("COORDINADOR");
        when(contextos.obtener(profileId)).thenReturn(context(null,
                List.of(new ContextoInstitucionalResponse.Adscripcion("CARRERA", careerA, true))));
        assertDoesNotThrow(() -> policy.validarCarrera(careerA));
    }

    @Test void coordinatorCareerAResourceCareerBForbidden() {
        UUID careerA = UUID.randomUUID(); authenticate("COORDINADOR");
        when(contextos.obtener(profileId)).thenReturn(context(null,
                List.of(new ContextoInstitucionalResponse.Adscripcion("CARRERA", careerA, true))));
        assertThrows(AccessDeniedException.class, () -> policy.validarCarrera(UUID.randomUUID()));
    }

    private void authenticate(String role) {
        var principal = new JwtPrincipal(UUID.randomUUID(), profileId, "scope-user");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private ContextoInstitucionalResponse context(UUID floor,
            List<ContextoInstitucionalResponse.Adscripcion> affiliations) {
        return new ContextoInstitucionalResponse(profileId, true, true, List.of(),
                new ContextoInstitucionalResponse.Administrador(floor != null, true, floor, null, floor != null),
                affiliations);
    }
}
