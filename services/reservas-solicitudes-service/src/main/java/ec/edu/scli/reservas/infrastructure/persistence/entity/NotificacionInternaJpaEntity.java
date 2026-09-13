package ec.edu.scli.reservas.infrastructure.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="notificaciones_internas")
public class NotificacionInternaJpaEntity {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 private UUID perfilId; private String titulo; @Column(columnDefinition="TEXT") private String cuerpo;
 private String tipo; private UUID referenciaId; private String claveEvento; private boolean leida; private Instant creadaEn; private Instant leidaEn;
 @PrePersist void crear(){if(creadaEn==null)creadaEn=Instant.now();}
 public UUID getId(){return id;} public UUID getPerfilId(){return perfilId;} public void setPerfilId(UUID v){perfilId=v;}
 public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;} public String getCuerpo(){return cuerpo;} public void setCuerpo(String v){cuerpo=v;}
 public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;} public UUID getReferenciaId(){return referenciaId;} public void setReferenciaId(UUID v){referenciaId=v;}
 public String getClaveEvento(){return claveEvento;} public void setClaveEvento(String v){claveEvento=v;}
 public boolean isLeida(){return leida;} public void setLeida(boolean v){leida=v;} public Instant getCreadaEn(){return creadaEn;} public Instant getLeidaEn(){return leidaEn;} public void setLeidaEn(Instant v){leidaEn=v;}
}
