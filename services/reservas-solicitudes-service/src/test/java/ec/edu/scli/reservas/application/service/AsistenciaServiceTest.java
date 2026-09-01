package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.domain.model.EstadoSesionAsistencia;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SesionAsistenciaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.RegistroAsistenciaJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.SesionAsistenciaJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.AbrirSesionAsistenciaRequest;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarAsistenciaRequest;
import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
import ec.edu.scli.reservas.domain.port.out.EstudianteInstitucionalPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsistenciaServiceTest {
    private SesionAsistenciaJpaRepository sesiones;
    private RegistroAsistenciaJpaRepository registros;
    private ReservaService reservas;
    private AsistenciaService service;
    private EstudianteInstitucionalPort estudiantes;

    @BeforeEach void preparar() {
        sesiones = mock(SesionAsistenciaJpaRepository.class);
        registros = mock(RegistroAsistenciaJpaRepository.class);
        reservas = mock(ReservaService.class);
        estudiantes = mock(EstudianteInstitucionalPort.class);
        when(estudiantes.resolverEstudianteActivo(any())).thenAnswer(i -> i.getArgument(0));
        service = new AsistenciaService(sesiones, registros, reservas, estudiantes, 15);
        when(sesiones.save(any())).thenAnswer(i -> i.getArgument(0));
        when(registros.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void propietarioAbreYAjenoNoPuede() {
        UUID reservaId = UUID.randomUUID(), docente = UUID.randomUUID();
        when(reservas.buscarPorId(reservaId)).thenReturn(reserva(reservaId, docente));
        assertThat(service.abrir(new AbrirSesionAsistenciaRequest(reservaId), docente).token()).isNotBlank();
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(reservaId), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test void registraSoloUnaVezConTokenValido() throws Exception {
        UUID sesionId = UUID.randomUUID(), estudiante = UUID.randomUUID();
        SesionAsistenciaJpaEntity sesion = sesion(sesionId, "secreto", Instant.now().plusSeconds(60));
        when(sesiones.findById(sesionId)).thenReturn(Optional.of(sesion));
        assertThat(service.registrar(sesionId, new RegistrarAsistenciaRequest("secreto"), estudiante)).isNotNull();
        when(registros.existsBySesionIdAndEstudianteId(sesionId, estudiante)).thenReturn(true);
        assertThatThrownBy(() -> service.registrar(sesionId, new RegistrarAsistenciaRequest("secreto"), estudiante))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("registrada");
    }

    @Test void rechazaTokenInvalidoVencidoYCerrado() throws Exception {
        UUID id = UUID.randomUUID();
        SesionAsistenciaJpaEntity sesion = sesion(id, "correcto", Instant.now().plusSeconds(60));
        when(sesiones.findById(id)).thenReturn(Optional.of(sesion));
        assertThatThrownBy(() -> service.registrar(id, new RegistrarAsistenciaRequest("incorrecto"), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        sesion = sesion(id, "correcto", Instant.now().minusSeconds(1)); when(sesiones.findById(id)).thenReturn(Optional.of(sesion));
        assertThatThrownBy(() -> service.registrar(id, new RegistrarAsistenciaRequest("correcto"), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("vencida");
        sesion = sesion(id, "correcto", Instant.now().plusSeconds(60)); sesion.setEstado(EstadoSesionAsistencia.CERRADA);
        when(sesiones.findById(id)).thenReturn(Optional.of(sesion));
        assertThatThrownBy(() -> service.registrar(id, new RegistrarAsistenciaRequest("correcto"), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("abierta");
    }

    @Test void soloPropietarioConsultaListaYCierraEHistorialEsPropio() {
        UUID id = UUID.randomUUID(), docente = UUID.randomUUID(), estudiante = UUID.randomUUID();
        SesionAsistenciaJpaEntity sesion = new SesionAsistenciaJpaEntity(); sesion.setId(id); sesion.setDocenteId(docente);
        sesion.setReservaId(UUID.randomUUID()); sesion.setEstado(EstadoSesionAsistencia.ABIERTA);
        when(sesiones.findByIdAndDocenteId(id, docente)).thenReturn(Optional.of(sesion));
        service.consultar(id, docente); service.listar(id, docente); service.cerrar(id, docente); service.historial(estudiante);
        verify(registros).findByEstudianteId(estudiante);
        assertThatThrownBy(() -> service.consultar(id, UUID.randomUUID())).isInstanceOf(AccessDeniedException.class);
    }

    private SesionAsistenciaJpaEntity sesion(UUID id, String token, Instant expira) throws Exception {
        SesionAsistenciaJpaEntity s = new SesionAsistenciaJpaEntity(); s.setId(id); s.setEstado(EstadoSesionAsistencia.ABIERTA);
        s.setExpiraEn(expira); s.setTokenHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)))); return s;
    }

    private ReservaResponse reserva(UUID id, UUID responsable) {
        return new ReservaResponse(id, UUID.randomUUID(), UUID.randomUUID(), responsable,
                null, null, null, null, null, null, null, null);
    }
}
