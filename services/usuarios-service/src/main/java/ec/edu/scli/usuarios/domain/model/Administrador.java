package ec.edu.scli.usuarios.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Administrador {

    private UUID id;
    private Perfil perfil;
    private String codigoAdministrador;
    private String cargo;
    private UUID pisoId;
    private Boolean activo = true;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public String getCodigoAdministrador() { return codigoAdministrador; }
    public void setCodigoAdministrador(String codigoAdministrador) { this.codigoAdministrador = codigoAdministrador; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public UUID getPisoId() { return pisoId; }
    public void setPisoId(UUID pisoId) { this.pisoId = pisoId; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(OffsetDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
