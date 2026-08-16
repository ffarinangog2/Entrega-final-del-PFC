package ec.edu.scli.academico.entity;

import ec.edu.scli.academico.enums.EstadoEquipo;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "equipos")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "laboratorio_id", nullable = false)
    private UUID laboratorioId;

    @Column(name = "tipo_equipo_id", nullable = false)
    private UUID tipoEquipoId;

    @Column(name = "codigo_inventario", nullable = false, unique = true, length = 30)
    private String codigoInventario;

    @Column(name = "numero_serie", unique = true, length = 60)
    private String numeroSerie;

    @Column(name = "marca", length = 60)
    private String marca;

    @Column(name = "modelo", length = 60)
    private String modelo;

    @Column(name = "procesador", length = 100)
    private String procesador;

    @Column(name = "memoria_ram", length = 30)
    private String memoriaRam;

    @Column(name = "almacenamiento", length = 30)
    private String almacenamiento;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "direccion_mac", length = 17)
    private String direccionMac;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEquipo estado = EstadoEquipo.OPERATIVO;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void alPersistir() {
        OffsetDateTime ahora = OffsetDateTime.now();
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    @PreUpdate
    protected void alActualizar() {
        this.actualizadoEn = OffsetDateTime.now();
    }

    // ---------- Getters y Setters ----------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLaboratorioId() {
        return laboratorioId;
    }

    public void setLaboratorioId(UUID laboratorioId) {
        this.laboratorioId = laboratorioId;
    }

    public UUID getTipoEquipoId() {
        return tipoEquipoId;
    }

    public void setTipoEquipoId(UUID tipoEquipoId) {
        this.tipoEquipoId = tipoEquipoId;
    }

    public String getCodigoInventario() {
        return codigoInventario;
    }

    public void setCodigoInventario(String codigoInventario) {
        this.codigoInventario = codigoInventario;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getProcesador() {
        return procesador;
    }

    public void setProcesador(String procesador) {
        this.procesador = procesador;
    }

    public String getMemoriaRam() {
        return memoriaRam;
    }

    public void setMemoriaRam(String memoriaRam) {
        this.memoriaRam = memoriaRam;
    }

    public String getAlmacenamiento() {
        return almacenamiento;
    }

    public void setAlmacenamiento(String almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public void setDireccionIp(String direccionIp) {
        this.direccionIp = direccionIp;
    }

    public String getDireccionMac() {
        return direccionMac;
    }

    public void setDireccionMac(String direccionMac) {
        this.direccionMac = direccionMac;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoEquipo estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(OffsetDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
