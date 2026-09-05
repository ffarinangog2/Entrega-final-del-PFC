package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.infrastructure.client.AcademicoPeriodoClient;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.ContextoAcademicoEstudianteEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.ContextoAcademicoEstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;
import ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoAcademicoEstudianteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ContextoAcademicoEstudianteServiceTest {

    private EstudianteRepository estudiantes;
    private ContextoAcademicoEstudianteRepository contextos;
    private AcademicoPeriodoClient academico;
    private ContextoAcademicoEstudianteService service;

    private final UUID estudianteId = UUID.randomUUID();
    private final UUID perfilId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        estudiantes = mock(EstudianteRepository.class);
        contextos = mock(ContextoAcademicoEstudianteRepository.class);
        academico = mock(AcademicoPeriodoClient.class);
        service = new ContextoAcademicoEstudianteService(estudiantes, contextos, academico);

        var e = new Estudiante();
        e.setId(estudianteId);
        when(estudiantes.findById(estudianteId)).thenReturn(Optional.of(e));
        when(estudiantes.findByPerfilId(perfilId)).thenReturn(Optional.of(e));
        when(contextos.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("carrera activa -> permite autodeclarar contexto")
    void carreraActivaPermiteContexto() {
        UUID carreraId = UUID.randomUUID();
        UUID periodoVigente = UUID.randomUUID();
        when(academico.estadoCarrera(carreraId)).thenReturn(new AcademicoPeriodoClient.CarreraEstadoResponse(carreraId, true, true));
        when(academico.periodoVigente()).thenReturn(periodoVigente);
        when(contextos.findByEstudianteIdAndPeriodoId(estudianteId, periodoVigente)).thenReturn(Optional.empty());
        when(contextos.findByEstudianteIdOrderByCreadoEnDesc(estudianteId)).thenReturn(List.of());

        var res = service.autodeclarar(perfilId, new ContextoAcademicoEstudianteRequest(carreraId, periodoVigente, 5));

        assertThat(res).isNotNull();
        assertThat(res.estudianteId()).isEqualTo(estudianteId);
        assertThat(res.carreraId()).isEqualTo(carreraId);
        assertThat(res.periodoId()).isEqualTo(periodoVigente);
        assertThat(res.nivel()).isEqualTo(5);
        assertThat(res.activo()).isTrue();
    }

    @Test
    @DisplayName("carrera inexistente -> rechaza con ResourceNotFoundException")
    void carreraInexistenteRechaza() {
        UUID carreraId = UUID.randomUUID();
        when(academico.estadoCarrera(carreraId)).thenReturn(new AcademicoPeriodoClient.CarreraEstadoResponse(carreraId, false, false));

        assertThatThrownBy(() -> service.autodeclarar(perfilId, new ContextoAcademicoEstudianteRequest(carreraId, UUID.randomUUID(), 4)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("carrera inactiva -> rechaza con BusinessRuleException")
    void carreraInactivaRechaza() {
        UUID carreraId = UUID.randomUUID();
        when(academico.estadoCarrera(carreraId)).thenReturn(new AcademicoPeriodoClient.CarreraEstadoResponse(carreraId, true, false));

        assertThatThrownBy(() -> service.autodeclarar(perfilId, new ContextoAcademicoEstudianteRequest(carreraId, UUID.randomUUID(), 4)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    @DisplayName("nivel inválido (< 1 o > 10) -> rechaza con BusinessRuleException")
    void nivelInvalidoRechaza() {
        UUID carreraId = UUID.randomUUID();

        assertThatThrownBy(() -> service.autodeclarar(perfilId, new ContextoAcademicoEstudianteRequest(carreraId, UUID.randomUUID(), 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("entre 1 y 10");

        assertThatThrownBy(() -> service.autodeclarar(perfilId, new ContextoAcademicoEstudianteRequest(carreraId, UUID.randomUUID(), 11)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("entre 1 y 10");
    }

    @Test
    @DisplayName("periodo enviado por cliente no sustituye al vigente determinado por backend")
    void periodoEnviadoPorClienteNoSustituyeAlVigente() {
        UUID carreraId = UUID.randomUUID();
        UUID periodoVigenteBackend = UUID.randomUUID();
        UUID periodoEnviadoCliente = UUID.randomUUID();

        when(academico.estadoCarrera(carreraId)).thenReturn(new AcademicoPeriodoClient.CarreraEstadoResponse(carreraId, true, true));
        when(academico.periodoVigente()).thenReturn(periodoVigenteBackend);
        when(contextos.findByEstudianteIdAndPeriodoId(estudianteId, periodoVigenteBackend)).thenReturn(Optional.empty());
        when(contextos.findByEstudianteIdOrderByCreadoEnDesc(estudianteId)).thenReturn(List.of());

        var res = service.autodeclarar(perfilId, new ContextoAcademicoEstudianteRequest(carreraId, periodoEnviadoCliente, 6));

        assertThat(res.periodoId()).isEqualTo(periodoVigenteBackend);
        assertThat(res.periodoId()).isNotEqualTo(periodoEnviadoCliente);
    }

    @Test
    @DisplayName("estudiante modifica únicamente su propio contexto y no puede operar sobre otro estudiante")
    void estudianteOperaUnicamenteSobreSuPropioContexto() {
        UUID otroPerfilId = UUID.randomUUID();
        when(estudiantes.findByPerfilId(otroPerfilId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.autodeclarar(otroPerfilId, new ContextoAcademicoEstudianteRequest(UUID.randomUUID(), UUID.randomUUID(), 3)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe estudiante para el perfil autenticado");
    }

    @Test
    void nuevoCicloConservaHistoricoYDesactivaSoloElAnterior() {
        var anterior = contexto(UUID.randomUUID(), UUID.randomUUID(), 7, true);
        when(contextos.findByEstudianteIdOrderByCreadoEnDesc(estudianteId)).thenReturn(List.of(anterior));
        when(contextos.findByEstudianteIdAndPeriodoId(eq(estudianteId), any())).thenReturn(Optional.empty());
        UUID carrera = UUID.randomUUID(), periodo = UUID.randomUUID();
        var nuevo = service.asignar(estudianteId, new ContextoAcademicoEstudianteRequest(carrera, periodo, 8));
        assertThat(anterior.getActivo()).isFalse();
        assertThat(nuevo.nivel()).isEqualTo(8);
        assertThat(nuevo.periodoId()).isEqualTo(periodo);
        verify(contextos, atLeast(2)).save(any());
    }

    @Test
    void selectorHistoricoNoModificaContextos() {
        var actual = contexto(UUID.randomUUID(), UUID.randomUUID(), 8, true);
        var anterior = contexto(UUID.randomUUID(), UUID.randomUUID(), 7, false);
        when(contextos.findByEstudianteIdOrderByCreadoEnDesc(estudianteId)).thenReturn(List.of(actual, anterior));
        assertThat(service.historial(perfilId)).extracting("nivel").containsExactly(8, 7);
        verify(contextos, never()).save(any());
    }

    @Test
    void estudianteNoPuedeReescribirContextoConfirmado() {
        UUID carreraId = UUID.randomUUID();
        UUID periodo = UUID.randomUUID();
        when(academico.estadoCarrera(carreraId)).thenReturn(new AcademicoPeriodoClient.CarreraEstadoResponse(carreraId, true, true));
        when(academico.periodoVigente()).thenReturn(periodo);
        when(contextos.findByEstudianteIdAndPeriodoId(estudianteId, periodo)).thenReturn(Optional.of(contexto(carreraId, periodo, 3, true)));

        assertThatThrownBy(() -> service.autodeclarar(perfilId, new ContextoAcademicoEstudianteRequest(carreraId, periodo, 8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("correccion administrativa");
    }

    private ContextoAcademicoEstudianteEntity contexto(UUID carrera, UUID periodo, int nivel, boolean activo) {
        var c = new ContextoAcademicoEstudianteEntity();
        c.setEstudianteId(estudianteId);
        c.setCarreraId(carrera);
        c.setPeriodoId(periodo);
        c.setNivel(nivel);
        c.setActivo(activo);
        return c;
    }
}
