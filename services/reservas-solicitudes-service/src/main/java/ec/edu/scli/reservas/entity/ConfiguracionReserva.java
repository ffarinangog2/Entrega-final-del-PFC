package ec.edu.scli.reservas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "configuraciones_reserva")
public class ConfiguracionReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "anticipacion_minima_horas", nullable = false)
    private Integer anticipacionMinimaHoras;

    @Column(name = "anticipacion_maxima_dias", nullable = false)
    private Integer anticipacionMaximaDias;

    @Column(name = "duracion_minima_minutos", nullable = false)
    private Integer duracionMinimaMinutos;

    @Column(name = "duracion_maxima_minutos", nullable = false)
    private Integer duracionMaximaMinutos;

    @Column(name = "permite_fines_semana", nullable = false)
    private Boolean permiteFinesSemana = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @UpdateTimestamp
    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ConfiguracionReserva() { }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Integer getAnticipacionMinimaHoras() { return anticipacionMinimaHoras; }
    public void setAnticipacionMinimaHoras(Integer anticipacionMinimaHoras) { this.anticipacionMinimaHoras = anticipacionMinimaHoras; }
    public Integer getAnticipacionMaximaDias() { return anticipacionMaximaDias; }
    public void setAnticipacionMaximaDias(Integer anticipacionMaximaDias) { this.anticipacionMaximaDias = anticipacionMaximaDias; }
    public Integer getDuracionMinimaMinutos() { return duracionMinimaMinutos; }
    public void setDuracionMinimaMinutos(Integer duracionMinimaMinutos) { this.duracionMinimaMinutos = duracionMinimaMinutos; }
    public Integer getDuracionMaximaMinutos() { return duracionMaximaMinutos; }
    public void setDuracionMaximaMinutos(Integer duracionMaximaMinutos) { this.duracionMaximaMinutos = duracionMaximaMinutos; }
    public Boolean getPermiteFinesSemana() { return permiteFinesSemana; }
    public void setPermiteFinesSemana(Boolean permiteFinesSemana) { this.permiteFinesSemana = permiteFinesSemana; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(Instant actualizadaEn) { this.actualizadaEn = actualizadaEn; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
