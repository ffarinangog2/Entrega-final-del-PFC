package ec.edu.scli.reservas.application.service;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.infrastructure.persistence.entity.IncidenteJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.IncidenteSpringDataRepository;
import ec.edu.scli.reservas.presentation.dto.request.CrearIncidenteRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate; import java.util.*;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class IncidenteServiceTest {
 @Mock IncidenteSpringDataRepository repository; @Mock NotificacionService notificaciones;
 IncidenteService service;
 @BeforeEach void init(){service=new IncidenteService(repository,notificaciones);}
 @Test void crearInfiereReportanteYEstado(){UUID actor=UUID.randomUUID();
  when(repository.saveAndFlush(any())).thenAnswer(i->{IncidenteJpaEntity e=i.getArgument(0);e.setId(UUID.randomUUID());e.setVersion(0L);return e;});
  var response=service.crear(new CrearIncidenteRequest(" Lab 1 "," Falla ",PrioridadIncidente.ALTA,LocalDate.now()),actor);
  assertEquals(actor,response.reportanteId());assertEquals("Lab 1",response.laboratorioEquipo());assertEquals(EstadoIncidente.REPORTADO,response.estado());
 }
 @Test void propietarioPuedeLeerPeroTerceroNo(){UUID owner=UUID.randomUUID();var e=entity(owner);when(repository.findById(e.getId())).thenReturn(Optional.of(e));
  assertDoesNotThrow(()->service.obtener(e.getId(),owner,false));
  assertThrows(AccessDeniedException.class,()->service.obtener(e.getId(),UUID.randomUUID(),false));
 }
 @Test void transicionInvalidaProduceConflicto(){var e=entity(UUID.randomUUID());when(repository.findById(e.getId())).thenReturn(Optional.of(e));
  assertThrows(IllegalStateException.class,()->service.cambiarEstado(e.getId(),EstadoIncidente.RESUELTO));
 }
 private IncidenteJpaEntity entity(UUID owner){var e=new IncidenteJpaEntity();e.setId(UUID.randomUUID());e.setReportanteId(owner);e.setLaboratorioEquipo("L1");e.setDescripcion("Falla");e.setPrioridad(PrioridadIncidente.MEDIA);e.setFecha(LocalDate.now());e.setVersion(0L);return e;}
}
