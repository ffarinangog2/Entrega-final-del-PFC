package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.application.usecase.PerfilService;
import ec.edu.scli.usuarios.infrastructure.client.AuthAdminClient;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilCreateRequest;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilUpdateRequest;
import ec.edu.scli.usuarios.presentation.dto.usuarios.UsuarioInstitucionalCreateRequest;
import ec.edu.scli.usuarios.presentation.dto.usuarios.UsuarioInstitucionalUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioInstitucionalAdminServiceTest {
    @Mock PerfilService perfiles;
    @Mock AsociacionRolService asociaciones;
    @Mock AuthAdminClient auth;
    @Mock CompensacionAuthTransaccional compensaciones;
    private UsuarioInstitucionalAdminService service;
    private UUID perfilId;
    private PerfilResponse perfil;

    @BeforeEach
    void setUp() {
        service = new UsuarioInstitucionalAdminService(perfiles, asociaciones, auth, compensaciones);
        perfilId = UUID.randomUUID();
        perfil = new PerfilResponse(perfilId, "0102030405", "Ana", "Piso", "ana@scli.edu.ec",
                null, null, null, null, null, true, null, null);
    }

    @Test
    void crearEjecutaPerfilAsociacionYAuthEnOrdenSeguro() {
        when(perfiles.crear(any())).thenReturn(perfil);
        var request = crearRequest("ADMINISTRADOR_PISO", UUID.randomUUID(), null);

        service.crear(request);

        InOrder orden = inOrder(perfiles, asociaciones, auth);
        orden.verify(perfiles).crear(request.perfil());
        orden.verify(asociaciones).asociar(eq(perfilId), any());
        orden.verify(auth).crear(any());
    }

    @Test
    void asociacionFallidaImpideCrearAuth() {
        when(perfiles.crear(any())).thenReturn(perfil);
        doThrow(new IllegalArgumentException("Carrera inválida")).when(asociaciones).asociar(eq(perfilId), any());

        assertThatThrownBy(() -> service.crear(crearRequest("COORDINADOR", null, UUID.randomUUID())))
                .hasMessage("Carrera inválida");
        verifyNoInteractions(auth);
    }

    @Test
    void authFallidoPropagaErrorParaQueSpringReviertaPerfilYAsociacion() {
        when(perfiles.crear(any())).thenReturn(perfil);
        doThrow(new IllegalStateException("Auth no disponible")).when(auth).crear(any());

        assertThatThrownBy(() -> service.crear(crearRequest("ADMINISTRADOR_PISO", UUID.randomUUID(), null)))
                .hasMessage("Auth no disponible");
    }

    @Test
    void cambioDeRolNoLlegaAAuthSiFallaLaAsociacion() {
        when(perfiles.actualizar(eq(perfilId), any())).thenReturn(perfil);
        doThrow(new IllegalArgumentException("Piso inválido")).when(asociaciones).asociar(eq(perfilId), any());

        assertThatThrownBy(() -> service.actualizar(perfilId, actualizarRequest(true)))
                .hasMessage("Piso inválido");
        verifyNoInteractions(auth);
    }

    @Test
    void desactivacionAuthFallidaPropagaErrorYRevierteTransaccionLocal() {
        when(perfiles.actualizar(eq(perfilId), any())).thenReturn(perfil);
        doThrow(new IllegalStateException("Auth no disponible")).when(auth).actualizar(any(), any());

        assertThatThrownBy(() -> service.actualizar(perfilId, actualizarRequest(false)))
                .hasMessage("Auth no disponible");
        verify(perfiles).cambiarEstado(perfilId, false);
    }

    private UsuarioInstitucionalCreateRequest crearRequest(String rol, UUID pisoId, UUID carreraId) {
        return new UsuarioInstitucionalCreateRequest(
                new PerfilCreateRequest("0102030405", "Ana", "Piso", "ana@scli.edu.ec", null, null, null, null, null),
                "ana.piso", "ana@scli.edu.ec", "ClaveSegura1!", rol, pisoId, carreraId);
    }

    private UsuarioInstitucionalUpdateRequest actualizarRequest(boolean activo) {
        return new UsuarioInstitucionalUpdateRequest(UUID.randomUUID(),
                new PerfilUpdateRequest("0102030405", "Ana", "Piso", "ana@scli.edu.ec", null, null, null, null, null),
                "ana.piso", "ana@scli.edu.ec", "ADMINISTRADOR_PISO", activo, UUID.randomUUID(), null);
    }
}
