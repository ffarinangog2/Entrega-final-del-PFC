package ec.edu.scli.reservas.application.service;
import ec.edu.scli.reservas.domain.port.out.NotificationPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.DispositivoNotificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.DispositivoNotificacionRepository;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarDispositivoRequest;
import ec.edu.scli.reservas.presentation.dto.response.DispositivoNotificacionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class NotificacionService {
    private final DispositivoNotificacionRepository repository; private final NotificationPort sender;
    public NotificacionService(DispositivoNotificacionRepository repository,NotificationPort sender){this.repository=repository;this.sender=sender;}
    @Transactional public DispositivoNotificacionResponse registrar(RegistrarDispositivoRequest request,UUID usuarioId,UUID perfilId){
        var e=repository.findByToken(request.token()).orElseGet(DispositivoNotificacionJpaEntity::new);
        e.setUsuarioAuthId(usuarioId);e.setPerfilId(perfilId);e.setToken(request.token());e.setPlataforma(request.plataforma());e.setActivo(true);
        return response(repository.saveAndFlush(e));
    }
    @Transactional public void desregistrar(String token, UUID perfilId) {
        repository.findByTokenAndPerfilId(token, perfilId).ifPresent(dispositivo -> {
            if (dispositivo.isActivo()) {
                dispositivo.setActivo(false);
                repository.save(dispositivo);
            }
        });
    }
    public void notificarPerfil(UUID perfilId,String titulo,String cuerpo,Map<String,String> datos){
        repository.findByPerfilIdAndActivoTrue(perfilId).forEach(d -> {
            try { sender.enviar(d.getToken(),titulo,cuerpo,datos); }
            catch (RuntimeException ignored) { /* La entrega push no revierte la operación de negocio. */ }
        });
    }
    private DispositivoNotificacionResponse response(DispositivoNotificacionJpaEntity e){return new DispositivoNotificacionResponse(e.getId(),e.getPlataforma(),e.isActivo(),e.getCreadoEn(),e.getActualizadoEn());}
}
