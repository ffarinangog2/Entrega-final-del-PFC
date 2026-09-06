package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoParticipanteUso;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "participantes_uso_laboratorio")
public class ParticipanteUsoLaboratorioJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID sesionId, estudiantePerfilId, registradoPorPerfilId;
    @Enumerated(EnumType.STRING) private EstadoParticipanteUso estado = EstadoParticipanteUso.PENDIENTE;
    private Instant registradoEn;
    private String observacion;
    @Version private Long version;
    public UUID getId(){return id;}
    public UUID getSesionId(){return sesionId;} public void setSesionId(UUID v){sesionId=v;}
    public UUID getEstudiantePerfilId(){return estudiantePerfilId;} public void setEstudiantePerfilId(UUID v){estudiantePerfilId=v;}
    public EstadoParticipanteUso getEstado(){return estado;} public void setEstado(EstadoParticipanteUso v){estado=v;}
    public Instant getRegistradoEn(){return registradoEn;} public void setRegistradoEn(Instant v){registradoEn=v;}
    public UUID getRegistradoPorPerfilId(){return registradoPorPerfilId;} public void setRegistradoPorPerfilId(UUID v){registradoPorPerfilId=v;}
    public String getObservacion(){return observacion;} public void setObservacion(String v){observacion=v;}
    public Long getVersion(){return version;}
}
