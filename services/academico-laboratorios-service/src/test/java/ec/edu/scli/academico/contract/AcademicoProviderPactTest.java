package ec.edu.scli.academico.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.scli.academico.application.facade.LaboratorioDetalleFacade;
import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.application.service.PeriodoLectivoService;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.infrastructure.observability.PrometheusQueryClient;
import ec.edu.scli.academico.presentation.controller.LaboratorioController;
import ec.edu.scli.academico.presentation.controller.MateriaController;
import ec.edu.scli.academico.presentation.controller.PeriodoLectivoController;
import ec.edu.scli.academico.security.PoliticaAmbitoAcademico;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueResponse;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioDetalleCompletoResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioResponse;
import ec.edu.scli.academico.presentation.dto.materia.MateriaResponse;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoResponse;
import ec.edu.scli.academico.presentation.dto.piso.PisoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("academico-laboratorios-service")
@PactFolder("../../tests/contract/target/pacts")
class AcademicoProviderPactTest {
    private static final UUID LAB_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private LaboratorioService laboratorios;
    private MateriaService materias;
    private PeriodoLectivoService periodos;
    private LaboratorioDetalleFacade detalle;

    @BeforeEach
    void target(PactVerificationContext context) {
        laboratorios = mock(LaboratorioService.class);
        materias = mock(MateriaService.class);
        periodos = mock(PeriodoLectivoService.class);
        detalle = mock(LaboratorioDetalleFacade.class);
        var mockMvc = MockMvcBuilders.standaloneSetup(
                        new LaboratorioController(laboratorios, detalle, mock(PrometheusQueryClient.class), mock(PoliticaAmbitoAcademico.class)),
                        new MateriaController(materias, mock(PoliticaAmbitoAcademico.class)), new PeriodoLectivoController(periodos))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().modulesToInstall(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .build();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("existen laboratorios activos")
    void laboratoriosActivos() {
        var response = laboratorio();
        when(laboratorios.listar(isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 100), 1));
    }

    @State("existen materias activas")
    void materiasActivas() {
        var response = new MateriaResponse(UUID.randomUUID(), UUID.randomUUID(), "MAT-01", "Redes",
                64, true, null, null);
        when(materias.listar(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 100), 1));
    }

    @State("existe un periodo lectivo activo")
    void periodoActivo() {
        when(periodos.obtenerActual()).thenReturn(new PeriodoLectivoResponse(UUID.randomUUID(), "2026-A",
                "Periodo 2026 A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                EstadoPeriodo.ACTIVO, null, null));
    }

    @State("existe el laboratorio para el QR")
    void laboratorioQr() {
        var pisoId = UUID.randomUUID();
        var bloqueId = UUID.randomUUID();
        var campusId = UUID.randomUUID();
        when(detalle.obtenerDetalleCompleto(LAB_ID)).thenReturn(new LaboratorioDetalleCompletoResponse(
                laboratorio(),
                new PisoResponse(pisoId, bloqueId, 2, "Planta alta", true, null, null),
                new BloqueResponse(bloqueId, campusId, "B1", "Bloque 1", true, null, null),
                new CampusResponse(campusId, "C1", "Central", "Campus central", true, null, null),
                List.of()));
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verificar(PactVerificationContext context) {
        context.verifyInteraction();
    }

    private LaboratorioResponse laboratorio() {
        return new LaboratorioResponse(LAB_ID, UUID.randomUUID(), "LAB-01", "Redes", 30,
                "Laboratorio de redes", EstadoLaboratorio.DISPONIBLE, true, null, null);
    }
}
