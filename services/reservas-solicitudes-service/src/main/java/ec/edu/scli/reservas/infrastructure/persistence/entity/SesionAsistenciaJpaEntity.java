package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoSesionAsistencia;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "sesiones_asistencia")
public class SesionAsistenciaJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID reservaId, bloquePlanificacionId, docenteId;
    private LocalDate fechaClase;
    private String tokenHash;
    private Instant abiertaEn, expiraEn, cerradaEn;
    @Enumerated(EnumType.STRING) private EstadoSesionAsistencia estado = EstadoSesionAsistencia.ABIERTA;
    private String temaActividad, observacionUso, diaSemanaSnapshot;
    private UUID carreraIdSnapshot, periodoIdSnapshot, materiaIdSnapshot, docenteIdSnapshot;
    private UUID laboratorioIdSnapshot, pisoIdSnapshot;
    private Integer nivelSnapshot;
    private LocalTime horaInicioSnapshot, horaFinSnapshot;
    @Version private Long version;

    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getReservaId(){return reservaId;} public void setReservaId(UUID v){reservaId=v;}
    public UUID getBloquePlanificacionId(){return bloquePlanificacionId;} public void setBloquePlanificacionId(UUID v){bloquePlanificacionId=v;}
    public UUID getDocenteId(){return docenteId;} public void setDocenteId(UUID v){docenteId=v;}
    public LocalDate getFechaClase(){return fechaClase;} public void setFechaClase(LocalDate v){fechaClase=v;}
    public String getTokenHash(){return tokenHash;} public void setTokenHash(String v){tokenHash=v;}
    public Instant getAbiertaEn(){return abiertaEn;} public void setAbiertaEn(Instant v){abiertaEn=v;}
    public Instant getExpiraEn(){return expiraEn;} public void setExpiraEn(Instant v){expiraEn=v;}
    public Instant getCerradaEn(){return cerradaEn;} public void setCerradaEn(Instant v){cerradaEn=v;}
    public EstadoSesionAsistencia getEstado(){return estado;} public void setEstado(EstadoSesionAsistencia v){estado=v;}
    public String getTemaActividad(){return temaActividad;} public void setTemaActividad(String v){temaActividad=v;}
    public String getObservacionUso(){return observacionUso;} public void setObservacionUso(String v){observacionUso=v;}
    public UUID getCarreraIdSnapshot(){return carreraIdSnapshot;} public void setCarreraIdSnapshot(UUID v){carreraIdSnapshot=v;}
    public UUID getPeriodoIdSnapshot(){return periodoIdSnapshot;} public void setPeriodoIdSnapshot(UUID v){periodoIdSnapshot=v;}
    public Integer getNivelSnapshot(){return nivelSnapshot;} public void setNivelSnapshot(Integer v){nivelSnapshot=v;}
    public UUID getMateriaIdSnapshot(){return materiaIdSnapshot;} public void setMateriaIdSnapshot(UUID v){materiaIdSnapshot=v;}
    public UUID getDocenteIdSnapshot(){return docenteIdSnapshot;} public void setDocenteIdSnapshot(UUID v){docenteIdSnapshot=v;}
    public UUID getLaboratorioIdSnapshot(){return laboratorioIdSnapshot;} public void setLaboratorioIdSnapshot(UUID v){laboratorioIdSnapshot=v;}
    public UUID getPisoIdSnapshot(){return pisoIdSnapshot;} public void setPisoIdSnapshot(UUID v){pisoIdSnapshot=v;}
    public String getDiaSemanaSnapshot(){return diaSemanaSnapshot;} public void setDiaSemanaSnapshot(String v){diaSemanaSnapshot=v;}
    public LocalTime getHoraInicioSnapshot(){return horaInicioSnapshot;} public void setHoraInicioSnapshot(LocalTime v){horaInicioSnapshot=v;}
    public LocalTime getHoraFinSnapshot(){return horaFinSnapshot;} public void setHoraFinSnapshot(LocalTime v){horaFinSnapshot=v;}
    public Long getVersion(){return version;}
}
