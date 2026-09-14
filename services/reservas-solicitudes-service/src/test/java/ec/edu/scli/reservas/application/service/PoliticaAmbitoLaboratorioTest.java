package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.domain.model.ContextoInstitucional;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PoliticaAmbitoLaboratorioTest {
    private ActorActualPort actorActual;
    private ContextoInstitucionalPort contextos;
    private AcademicoLaboratoriosClient academico;
    private PoliticaAmbitoLaboratorio politica;
    private final UUID perfil = UUID.randomUUID();
    private final UUID piso = UUID.randomUUID();
    private final UUID laboratorio = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        actorActual = mock(ActorActualPort.class);
        contextos = mock(ContextoInstitucionalPort.class);
        academico = mock(AcademicoLaboratoriosClient.class);
        politica = new PoliticaAmbitoLaboratorio(actorActual, contextos, academico);
        when(academico.obtenerLaboratorio(laboratorio)).thenReturn(
                new LaboratorioExternoResponse(laboratorio, piso, true, true, "ACTIVO", 30));
    }

    @Test
    void administradorDePisoConRolYContextoGestionaSoloSuPiso() {
        actor("ROLE_ADMINISTRADOR_PISO", "SOLICITUD_APROBAR");
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(contexto(true, true, true, piso));
        assertEquals(piso, politica.validarGestion(laboratorio));

        UUID otroLaboratorio = UUID.randomUUID();
        when(academico.obtenerLaboratorio(otroLaboratorio)).thenReturn(
                new LaboratorioExternoResponse(otroLaboratorio, UUID.randomUUID(), true, true, "ACTIVO", 20));
        assertThrows(AccessDeniedException.class, () -> politica.validarGestion(otroLaboratorio));
    }

    @Test
    void contextoInvalidoInactivoOSinPisoNuncaConcedeAmbito() {
        actor("ROLE_ADMINISTRADOR_PISO", "SOLICITUD_APROBAR");
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(contexto(false, true, true, piso));
        assertThrows(AccessDeniedException.class, () -> politica.pisoGestionado());
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(contexto(true, false, true, piso));
        assertThrows(AccessDeniedException.class, () -> politica.pisoGestionado());
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(contexto(true, true, false, null));
        assertThrows(AccessDeniedException.class, () -> politica.pisoGestionado());
    }

    @Test
    void contextoAusenteInexistenteOAdministradorIncompletoNoConcedeAmbito() {
        actor("ROLE_ADMINISTRADOR_PISO", "SOLICITUD_APROBAR");
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(null);
        assertThrows(AccessDeniedException.class, () -> politica.pisoGestionado());
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(
                new ContextoInstitucional(false, true, false, false, false, null));
        assertThrows(AccessDeniedException.class, () -> politica.pisoGestionado());
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(
                new ContextoInstitucional(true, true, false, false, false, null));
        assertThrows(AccessDeniedException.class, () -> politica.pisoGestionado());
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(
                new ContextoInstitucional(true, true, false, true, true, piso));
        assertThrows(AccessDeniedException.class, () -> politica.pisoGestionado());
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(contexto(true, true, true, null));
        assertThrows(AccessDeniedException.class, () -> politica.pisoGestionado());
    }

    @Test
    void laboratorioInexistenteOSinPisoNoPuedeGestionarse() {
        actor("ROLE_ADMINISTRADOR", "SOLICITUD_APROBAR");
        when(academico.obtenerLaboratorio(laboratorio)).thenReturn(null);
        assertThrows(AccessDeniedException.class, () -> politica.validarGestion(laboratorio));
        when(academico.obtenerLaboratorio(laboratorio)).thenReturn(
                new LaboratorioExternoResponse(laboratorio, null, false, false, "INACTIVO", 0));
        assertThrows(AccessDeniedException.class, () -> politica.validarGestion(laboratorio));
    }

    @Test
    void adscripcionSinRolNoConcedePrivilegio() {
        actor("ROLE_DOCENTE", "SOLICITUD_APROBAR");
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(contexto(true, true, true, piso));
        assertThrows(AccessDeniedException.class, () -> politica.validarGestion(laboratorio));
        verifyNoInteractions(contextos);
    }

    @Test
    void administradorGlobalConservaGestionGlobal() {
        actor("ROLE_ADMINISTRADOR", "SOLICITUD_APROBAR");
        assertEquals(piso, politica.validarGestion(laboratorio));
        verifyNoInteractions(contextos);
    }

    private void actor(String... authorities) {
        when(actorActual.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of(authorities)));
    }

    private ContextoInstitucional contexto(
            boolean perfilActivo, boolean administradorActivo, boolean operativo, UUID pisoId) {
        return new ContextoInstitucional(true, perfilActivo, true, administradorActivo, operativo, pisoId);
    }
}
