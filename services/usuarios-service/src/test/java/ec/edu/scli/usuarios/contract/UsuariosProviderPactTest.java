package ec.edu.scli.usuarios.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.scli.usuarios.application.usecase.PerfilService;
import ec.edu.scli.usuarios.presentation.controller.PerfilController;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.security.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Provider("usuarios-service")
@PactFolder("../../tests/contract/target/pacts")
class UsuariosProviderPactTest {

    private static final UUID PERFIL_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");

    private PerfilService perfilService;

    @BeforeEach
    void configurarTarget(PactVerificationContext context) {
        perfilService = mock(PerfilService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PerfilController(perfilService))
                .defaultRequest(get("/").principal(new JwtPrincipal(
                        UUID.randomUUID(), PERFIL_ID, "ana")))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("existe un perfil con id 55555555-5555-5555-5555-555555555555")
    void existePerfilPorId() {
        when(perfilService.obtenerPorId(PERFIL_ID)).thenReturn(perfilDeEjemplo());
    }

    @State("existen perfiles registrados")
    void existenPerfilesRegistrados() {
        Page<PerfilResponse> pagina = new PageImpl<>(
                List.of(perfilDeEjemplo()), PageRequest.of(0, 20), 1);
        when(perfilService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(pagina);
    }

    @State("el usuario autenticado tiene un perfil propio")
    void existePerfilPropio() {
        when(perfilService.obtenerPorId(PERFIL_ID)).thenReturn(perfilDeEjemplo());
        when(perfilService.actualizarPropio(any(), any())).thenReturn(perfilDeEjemplo());
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verificarInteraccion(PactVerificationContext context) {
        context.verifyInteraction();
    }

    private PerfilResponse perfilDeEjemplo() {
        OffsetDateTime instante = OffsetDateTime.parse("2026-08-18T10:00:00Z");
        return new PerfilResponse(
                PERFIL_ID,
                "0102030405",
                "Ana",
                "Gomez",
                "ana.gomez@uteq.edu.ec",
                "ana.gomez@gmail.com",
                "0999999999",
                "Av. Principal 123",
                LocalDate.parse("1995-05-20"),
                "https://cdn.scli.edu.ec/perfiles/ana.png",
                true,
                instante,
                instante
        );
    }
}
