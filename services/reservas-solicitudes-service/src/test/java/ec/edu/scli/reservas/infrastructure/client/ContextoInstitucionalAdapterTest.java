package ec.edu.scli.reservas.infrastructure.client;

import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.client.dto.ContextoInstitucionalExternoResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContextoInstitucionalAdapterTest {
    @Test
    void traduceContratoExternoSinFiltrarElDtoHaciaAplicacion() {
        UsuariosClient usuarios = mock(UsuariosClient.class);
        UUID perfil = UUID.randomUUID();
        UUID piso = UUID.randomUUID();
        when(usuarios.obtenerContextoInstitucional(perfil)).thenReturn(
                new ContextoInstitucionalExternoResponse(perfil, true, true, List.of("ADMINISTRADOR"),
                        new ContextoInstitucionalExternoResponse.Administrador(
                                true, true, piso, "Piso", true), List.of()));
        var contexto = new ContextoInstitucionalAdapter(usuarios).obtenerPorPerfilId(perfil);
        assertTrue(contexto.perfilActivo());
        assertTrue(contexto.administradorPisoOperativo());
        assertEquals(piso, contexto.pisoId());
    }

    @Test
    void conservaAusenciaDeContextoOAdministrador() {
        UsuariosClient usuarios = mock(UsuariosClient.class);
        UUID perfil = UUID.randomUUID();
        var adapter = new ContextoInstitucionalAdapter(usuarios);
        assertNull(adapter.obtenerPorPerfilId(perfil));
        when(usuarios.obtenerContextoInstitucional(perfil)).thenReturn(
                new ContextoInstitucionalExternoResponse(perfil, true, true, List.of(), null, List.of()));
        assertFalse(adapter.obtenerPorPerfilId(perfil).administradorExiste());
        assertNull(adapter.obtenerPorPerfilId(perfil).pisoId());
    }
}
