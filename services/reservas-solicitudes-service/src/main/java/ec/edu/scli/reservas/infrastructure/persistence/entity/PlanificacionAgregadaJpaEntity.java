package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "planificaciones", uniqueConstraints =
        @UniqueConstraint(name = "uq_planificacion_carrera_ciclo", columnNames = {"carrera_id", "periodo_id"}))
public class PlanificacionAgregadaJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID carreraId;
    private UUID periodoId;
    private UUID coordinadorPerfilId;
    @Enumerated(EnumType.STRING) private EstadoPlanificacionAgregada estado;
    private Instant creadaEn;
    private Instant actualizadaEn;
    private Instant enviadaEn;
    private Instant aprobadaEn;
    @Version private Long version;
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getCarreraId() { return carreraId; }
    public void setCarreraId(UUID value) { carreraId = value; }
    public UUID getPeriodoId() { return periodoId; }
    public void setPeriodoId(UUID value) { periodoId = value; }
    public UUID getCoordinadorPerfilId() { return coordinadorPerfilId; }
    public void setCoordinadorPerfilId(UUID value) { coordinadorPerfilId = value; }
    public EstadoPlanificacionAgregada getEstado() { return estado; }
    public void setEstado(EstadoPlanificacionAgregada value) { estado = value; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant value) { creadaEn = value; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(Instant value) { actualizadaEn = value; }
    public Instant getEnviadaEn() { return enviadaEn; }
    public void setEnviadaEn(Instant value) { enviadaEn = value; }
    public Instant getAprobadaEn() { return aprobadaEn; }
    public void setAprobadaEn(Instant value) { aprobadaEn = value; }
    public Long getVersion() { return version; }
}
