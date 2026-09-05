package ec.edu.uteq.scli.auth_service.application;

import ec.edu.uteq.scli.auth_service.application.service.LoginProtectionService;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginProtectionServiceTest {
 @Mock UsuarioAuthRepository users; @Mock IntentoLoginRepository attempts;
 private UsuarioAuth user; private Instant instant; private LoginProtectionService service;
 @BeforeEach void setup(){instant=Instant.parse("2026-01-01T00:00:00Z");service=new LoginProtectionService(users,attempts,Clock.fixed(instant,ZoneOffset.UTC));
  user=new UsuarioAuth();user.setId(UUID.randomUUID());user.setIntentosFallidos(0);user.setCuentaBloqueada(false);user.setActualizadoEn(OffsetDateTime.now());
  lenient().when(users.findLockedByUsernameIgnoreCase("user")).thenReturn(Optional.of(user));}
 @Test void fallosUnoACuatroNoBloquean(){for(int i=1;i<=4;i++){assertFalse(service.recordFailure("user",meta()));assertEquals(i,user.getIntentosFallidos());assertFalse(user.getCuentaBloqueada());}}
 @Test void quintoFalloBloqueaQuinceMinutos(){for(int i=0;i<4;i++)service.recordFailure("user",meta());assertTrue(service.recordFailure("user",meta()));assertEquals(5,user.getIntentosFallidos());assertEquals(OffsetDateTime.ofInstant(instant.plusSeconds(900),ZoneOffset.UTC),user.getBloqueadoHasta());}
 @Test void duranteBloqueoNoIncrementa(){user.setCuentaBloqueada(true);user.setIntentosFallidos(5);user.setBloqueadoHasta(OffsetDateTime.ofInstant(instant.plusSeconds(900),ZoneOffset.UTC));assertTrue(service.recordFailure("user",meta()));assertEquals(5,user.getIntentosFallidos());}
 @Test void exactamenteQuinceMinutosDesbloquea(){user.setCuentaBloqueada(true);user.setIntentosFallidos(5);user.setBloqueadoHasta(OffsetDateTime.ofInstant(instant,ZoneOffset.UTC));assertFalse(service.prepareLogin("user"));assertEquals(0,user.getIntentosFallidos());assertNull(user.getBloqueadoHasta());}
 @Test void loginExitosoReinicia(){when(users.findLockedById(user.getId())).thenReturn(Optional.of(user));user.setIntentosFallidos(3);service.recordSuccess(user.getId(),"user",meta());assertEquals(0,user.getIntentosFallidos());assertFalse(user.getCuentaBloqueada());}
 private LoginProtectionService.LoginMetadata meta(){return new LoginProtectionService.LoginMetadata("127.0.0.1","test");}
}
