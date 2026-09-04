package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.*;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.*;
import ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoAcademicoEstudianteRequest;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContextoAcademicoEstudianteServiceTest {
 private EstudianteRepository estudiantes; private ContextoAcademicoEstudianteRepository contextos; private ContextoAcademicoEstudianteService service;
 private final UUID estudianteId=UUID.randomUUID(),perfilId=UUID.randomUUID();
 @BeforeEach void setUp(){estudiantes=mock(EstudianteRepository.class);contextos=mock(ContextoAcademicoEstudianteRepository.class);service=new ContextoAcademicoEstudianteService(estudiantes,contextos);var e=new Estudiante();e.setId(estudianteId);when(estudiantes.findById(estudianteId)).thenReturn(Optional.of(e));when(estudiantes.findByPerfilId(perfilId)).thenReturn(Optional.of(e));when(contextos.save(any())).thenAnswer(i->i.getArgument(0));}
 @Test void nuevoCicloConservaHistoricoYDesactivaSoloElAnterior(){var anterior=contexto(UUID.randomUUID(),UUID.randomUUID(),7,true);when(contextos.findByEstudianteIdOrderByCreadoEnDesc(estudianteId)).thenReturn(List.of(anterior));when(contextos.findByEstudianteIdAndPeriodoId(eq(estudianteId),any())).thenReturn(Optional.empty());UUID carrera=UUID.randomUUID(),periodo=UUID.randomUUID();var nuevo=service.asignar(estudianteId,new ContextoAcademicoEstudianteRequest(carrera,periodo,8));assertThat(anterior.getActivo()).isFalse();assertThat(nuevo.nivel()).isEqualTo(8);assertThat(nuevo.periodoId()).isEqualTo(periodo);verify(contextos,atLeast(2)).save(any());}
 @Test void selectorHistoricoNoModificaContextos(){var actual=contexto(UUID.randomUUID(),UUID.randomUUID(),8,true);var anterior=contexto(UUID.randomUUID(),UUID.randomUUID(),7,false);when(contextos.findByEstudianteIdOrderByCreadoEnDesc(estudianteId)).thenReturn(List.of(actual,anterior));assertThat(service.historial(perfilId)).extracting("nivel").containsExactly(8,7);verify(contextos,never()).save(any());}
 private ContextoAcademicoEstudianteEntity contexto(UUID carrera,UUID periodo,int nivel,boolean activo){var c=new ContextoAcademicoEstudianteEntity();c.setEstudianteId(estudianteId);c.setCarreraId(carrera);c.setPeriodoId(periodo);c.setNivel(nivel);c.setActivo(activo);return c;}
}
