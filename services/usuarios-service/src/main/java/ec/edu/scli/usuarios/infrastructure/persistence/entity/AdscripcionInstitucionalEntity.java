package ec.edu.scli.usuarios.infrastructure.persistence.entity;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "adscripciones_institucionales")
public class AdscripcionInstitucionalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ambito", nullable = false, length = 20)
    private TipoAmbitoInstitucional tipoAmbito;

    @Column(name = "ambito_id", nullable = false)
    private UUID ambitoId;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    public UUID getId() { return id; }
    public Perfil getPerfil() { return perfil; }
    public TipoAmbitoInstitucional getTipoAmbito() { return tipoAmbito; }
    public UUID getAmbitoId() { return ambitoId; }
    public boolean isActivo() { return activo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
}
