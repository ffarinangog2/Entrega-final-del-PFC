package ec.edu.scli.usuarios.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Docente {

    private UUID id;
    private Perfil perfil;
    private String codigoDocente;
    private String tituloAcademico;
    private String departamento;
    private String tipoContrato;
    private String dedicacion;
    private Boolean activo = true;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public String getCodigoDocente() { return codigoDocente; }
    public void setCodigoDocente(String codigoDocente) { this.codigoDocente = codigoDocente; }
    public String getTituloAcademico() { return tituloAcademico; }
    public void setTituloAcademico(String tituloAcademico) { this.tituloAcademico = tituloAcademico; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public String getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(String tipoContrato) { this.tipoContrato = tipoContrato; }
    public String getDedicacion() { return dedicacion; }
    public void setDedicacion(String dedicacion) { this.dedicacion = dedicacion; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(OffsetDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
