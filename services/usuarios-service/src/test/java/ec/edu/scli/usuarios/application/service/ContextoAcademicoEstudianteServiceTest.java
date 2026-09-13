package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.infrastructure.client.AcademicoPeriodoClient;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.ContextoAcademicoEstudianteEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil;
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
        var p = new Perfil();
        p.setId(perfilId);
        e.setPerfil(p);

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

    @Test
    @DisplayName("listarContextosMasivos retorna perfilId, estudianteId, carreraId, periodoId, nivel y no modifica datos")
    void listarContextosMasivosRetornaDatosCompletosSinModificar() {
        UUID ppaId = UUID.randomUUID();
        UUID carreraId1 = UUID.randomUUID();
        UUID estudianteId2 = UUID.randomUUID();
        UUID perfilId2 = UUID.randomUUID();
        UUID carreraId2 = UUID.randomUUID();

        var e1 = new Estudiante();
        e1.setId(estudianteId);
        var p1 = new Perfil();
        p1.setId(perfilId);
        e1.setPerfil(p1);

        var e2 = new Estudiante();
        e2.setId(estudianteId2);
        var p2 = new Perfil();
        p2.setId(perfilId2);
        e2.setPerfil(p2);

        when(estudiantes.findAll()).thenReturn(List.of(e1, e2));

        var ctx1 = contexto(carreraId1, ppaId, 1, true);
        var ctx2 = new ContextoAcademicoEstudianteEntity();
        ctx2.setEstudianteId(estudianteId2);
        ctx2.setCarreraId(carreraId2);
        ctx2.setPeriodoId(ppaId);
        ctx2.setNivel(3);
        ctx2.setActivo(true);

        when(contextos.findByPeriodoId(ppaId)).thenReturn(List.of(ctx1, ctx2));

        var resultado = service.listarContextosMasivos(ppaId);

        assertThat(resultado).hasSize(2);

        var item1 = resultado.stream().filter(r -> r.estudianteId().equals(estudianteId)).findFirst().orElseThrow();
        assertThat(item1.perfilId()).isEqualTo(perfilId);
        assertThat(item1.carreraId()).isEqualTo(carreraId1);
        assertThat(item1.periodoId()).isEqualTo(ppaId);
        assertThat(item1.nivel()).isEqualTo(1);
        assertThat(item1.activo()).isTrue();

        var item2 = resultado.stream().filter(r -> r.estudianteId().equals(estudianteId2)).findFirst().orElseThrow();
        assertThat(item2.perfilId()).isEqualTo(perfilId2);
        assertThat(item2.carreraId()).isEqualTo(carreraId2);
        assertThat(item2.periodoId()).isEqualTo(ppaId);
        assertThat(item2.nivel()).isEqualTo(3);
        assertThat(item2.activo()).isTrue();

        // Demostrar que NO se modifican datos en base de datos
        verify(contextos, never()).save(any());
        verify(contextos, never()).delete(any());
        verify(estudiantes, never()).save(any());
    }

    @Test
    @DisplayName("listarContextosMasivos con periodoId filtra estrictamente por ese período sin fallback a otro")
    void listarContextosMasivosFiltraEstrictamenteSinFallback() {
        UUID periodoPpa = UUID.randomUUID();
        UUID periodoSpa = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();

        var e1 = new Estudiante();
        e1.setId(estudianteId);
        var p1 = new Perfil();
        p1.setId(perfilId);
        e1.setPerfil(p1);

        when(estudiantes.findAll()).thenReturn(List.of(e1));

        // El estudiante solo tiene contexto en SPA, NO en PPA
        when(contextos.findByPeriodoId(periodoPpa)).thenReturn(List.of());

        var resultado = service.listarContextosMasivos(periodoPpa);

        assertThat(resultado).isEmpty();
        // Verifica que no llama a findByActivoTrue ni a otro período
        verify(contextos, never()).findByActivoTrue();
        verify(contextos, never()).findByPeriodoId(periodoSpa);
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
