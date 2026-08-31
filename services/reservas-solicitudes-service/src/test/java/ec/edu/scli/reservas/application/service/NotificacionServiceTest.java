package ec.edu.scli.reservas.application.service;
import ec.edu.scli.reservas.domain.port.out.NotificationPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.DispositivoNotificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.DispositivoNotificacionRepository;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarDispositivoRequest;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {
 @Mock DispositivoNotificacionRepository repository; @Mock NotificationPort sender; NotificacionService service;
 @BeforeEach void init(){service=new NotificacionService(repository,sender);}
 @Test void registrarAsociaIdentidadAutenticadaYReactivaToken(){UUID user=UUID.randomUUID(),profile=UUID.randomUUID();var e=new DispositivoNotificacionJpaEntity();
  when(repository.findByToken("token")).thenReturn(Optional.of(e));when(repository.saveAndFlush(e)).thenReturn(e);
  service.registrar(new RegistrarDispositivoRequest("token","ANDROID"),user,profile);
  assertEquals(user,e.getUsuarioAuthId());assertEquals(profile,e.getPerfilId());assertTrue(e.isActivo());
 }
 @Test void senderNoPropagaFalloNiExponeToken(){UUID profile=UUID.randomUUID();var e=new DispositivoNotificacionJpaEntity();e.setToken("secreto");
  when(repository.findByPerfilIdAndActivoTrue(profile)).thenReturn(List.of(e));doThrow(new IllegalStateException()).when(sender).enviar(any(),any(),any(),any());
  assertDoesNotThrow(()->service.notificarPerfil(profile,"t","c",Map.of()));
 }
 @Test void desregistrarSoloDesactivaTokenDelPerfilAutenticado(){UUID profile=UUID.randomUUID();var e=new DispositivoNotificacionJpaEntity();e.setActivo(true);
  when(repository.findByTokenAndPerfilId("token",profile)).thenReturn(Optional.of(e));
  service.desregistrar("token",profile);
  assertFalse(e.isActivo());verify(repository).save(e);
 }
 @Test void desregistrarEsIdempotenteYNoAfectaTokenAjeno(){UUID profile=UUID.randomUUID();
  when(repository.findByTokenAndPerfilId("token",profile)).thenReturn(Optional.empty());
  assertDoesNotThrow(()->service.desregistrar("token",profile));verify(repository,never()).save(any());
 }
}
