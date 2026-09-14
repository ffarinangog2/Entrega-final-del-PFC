package ec.edu.scli.reservas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="dispositivos_notificacion")
public class DispositivoNotificacionJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="usuario_auth_id",nullable=false) private UUID usuarioAuthId;
    @Column(name="perfil_id",nullable=false) private UUID perfilId;
    @Column(nullable=false,length=4096,unique=true) private String token;
    @Column(nullable=false,length=20) private String plataforma;
    @Column(nullable=false) private boolean activo=true;
    @CreationTimestamp @Column(name="creado_en",nullable=false,updatable=false) private Instant creadoEn;
    @UpdateTimestamp @Column(name="actualizado_en",nullable=false) private Instant actualizadoEn;
    public UUID getId(){return id;} public UUID getUsuarioAuthId(){return usuarioAuthId;} public void setUsuarioAuthId(UUID v){usuarioAuthId=v;}
    public UUID getPerfilId(){return perfilId;} public void setPerfilId(UUID v){perfilId=v;}
    public String getToken(){return token;} public void setToken(String v){token=v;}
    public String getPlataforma(){return plataforma;} public void setPlataforma(String v){plataforma=v;}
    public boolean isActivo(){return activo;} public void setActivo(boolean v){activo=v;}
    public Instant getCreadoEn(){return creadoEn;} public Instant getActualizadoEn(){return actualizadoEn;}
}
