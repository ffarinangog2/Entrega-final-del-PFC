package ec.edu.scli.reservas.application.service;
import ec.edu.scli.reservas.domain.port.out.NotificationPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.DispositivoNotificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.DispositivoNotificacionRepository;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarDispositivoRequest;
import ec.edu.scli.reservas.presentation.dto.response.DispositivoNotificacionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;
import ec.edu.scli.reservas.infrastructure.persistence.repository.NotificacionInternaJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.entity.NotificacionInternaJpaEntity;
import ec.edu.scli.reservas.presentation.dto.response.NotificacionInternaResponse;
import org.springframework.beans.factory.annotation.Autowired;
@Service
public class NotificacionService {
    private final DispositivoNotificacionRepository repository; private final NotificationPort sender; private final NotificacionInternaJpaRepository bandeja;
    public NotificacionService(DispositivoNotificacionRepository repository,NotificationPort sender){this(repository,sender,null);}
    @Autowired public NotificacionService(DispositivoNotificacionRepository repository,NotificationPort sender,NotificacionInternaJpaRepository bandeja){this.repository=repository;this.sender=sender;this.bandeja=bandeja;}
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
        notificarPerfilIdempotente(perfilId,null,titulo,cuerpo,datos);
    }
    @Transactional public void notificarPerfilIdempotente(UUID perfilId,String clave,String titulo,String cuerpo,Map<String,String> datos){
        if(bandeja!=null && (clave==null || !bandeja.existsByClaveEvento(clave))){var n=new NotificacionInternaJpaEntity();n.setPerfilId(perfilId);n.setTitulo(titulo);n.setCuerpo(cuerpo);n.setTipo(datos.get("tipo"));n.setClaveEvento(clave);try{String ref=datos.getOrDefault("planificacionId",datos.get("referenciaId"));if(ref!=null)n.setReferenciaId(UUID.fromString(ref));}catch(IllegalArgumentException ignored){}bandeja.save(n);} else if(clave!=null){return;}
        repository.findByPerfilIdAndActivoTrue(perfilId).forEach(d -> {
            try { sender.enviar(d.getToken(),titulo,cuerpo,datos); }
            catch (RuntimeException ignored) { /* La entrega push no revierte la operación de negocio. */ }
        });
    }
    @Transactional(readOnly=true) public List<NotificacionInternaResponse> listar(UUID perfilId){return bandeja.findTop50ByPerfilIdOrderByCreadaEnDesc(perfilId).stream().map(this::response).toList();}
    @Transactional(readOnly=true) public long noLeidas(UUID perfilId){return bandeja.countByPerfilIdAndLeidaFalse(perfilId);}
    @Transactional public NotificacionInternaResponse leer(UUID id,UUID perfilId){var n=bandeja.findByIdAndPerfilId(id,perfilId).orElseThrow();n.setLeida(true);n.setLeidaEn(Instant.now());return response(bandeja.save(n));}
    @Transactional public void leerTodas(UUID perfilId){var items=bandeja.findByPerfilIdAndLeidaFalse(perfilId);items.forEach(n->{n.setLeida(true);n.setLeidaEn(Instant.now());});bandeja.saveAll(items);}
    private NotificacionInternaResponse response(NotificacionInternaJpaEntity n){return new NotificacionInternaResponse(n.getId(),n.getTitulo(),n.getCuerpo(),n.getTipo(),n.getReferenciaId(),n.isLeida(),n.getCreadaEn());}
    private DispositivoNotificacionResponse response(DispositivoNotificacionJpaEntity e){return new DispositivoNotificacionResponse(e.getId(),e.getPlataforma(),e.isActivo(),e.getCreadoEn(),e.getActualizadoEn());}
}
