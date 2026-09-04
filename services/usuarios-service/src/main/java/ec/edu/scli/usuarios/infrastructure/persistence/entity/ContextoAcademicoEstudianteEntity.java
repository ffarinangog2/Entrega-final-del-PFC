package ec.edu.scli.usuarios.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contextos_academicos_estudiante", uniqueConstraints =
        @UniqueConstraint(name = "uq_contexto_estudiante_periodo", columnNames = {"estudiante_id", "periodo_id"}))
public class ContextoAcademicoEstudianteEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID estudianteId;
    @Column(nullable = false) private UUID carreraId;
    @Column(nullable = false) private UUID periodoId;
    @Column(nullable = false) private Integer nivel;
    @Column(nullable = false) private Boolean activo = true;
    @Column(nullable = false, updatable = false) private OffsetDateTime creadoEn;
    @PrePersist void crear() { if (creadoEn == null) creadoEn = OffsetDateTime.now(); }
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getEstudianteId(){return estudianteId;} public void setEstudianteId(UUID v){estudianteId=v;}
    public UUID getCarreraId(){return carreraId;} public void setCarreraId(UUID v){carreraId=v;}
    public UUID getPeriodoId(){return periodoId;} public void setPeriodoId(UUID v){periodoId=v;}
    public Integer getNivel(){return nivel;} public void setNivel(Integer v){nivel=v;}
    public Boolean getActivo(){return activo;} public void setActivo(Boolean v){activo=v;}
    public OffsetDateTime getCreadoEn(){return creadoEn;}
}
