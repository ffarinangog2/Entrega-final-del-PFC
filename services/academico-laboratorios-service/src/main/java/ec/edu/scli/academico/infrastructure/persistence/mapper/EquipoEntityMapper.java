package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.Equipo;
import ec.edu.scli.academico.infrastructure.persistence.entity.EquipoEntity;
import org.springframework.stereotype.Component;

@Component
public class EquipoEntityMapper {

    public EquipoEntity aEntidad(Equipo dominio) {
        EquipoEntity entidad = new EquipoEntity();
        entidad.setId(dominio.getId());
        entidad.setLaboratorioId(dominio.getLaboratorioId());
        entidad.setTipoEquipoId(dominio.getTipoEquipoId());
        entidad.setCodigoInventario(dominio.getCodigoInventario());
        entidad.setNumeroSerie(dominio.getNumeroSerie());
        entidad.setMarca(dominio.getMarca());
        entidad.setModelo(dominio.getModelo());
        entidad.setProcesador(dominio.getProcesador());
        entidad.setMemoriaRam(dominio.getMemoriaRam());
        entidad.setAlmacenamiento(dominio.getAlmacenamiento());
        entidad.setDireccionIp(dominio.getDireccionIp());
        entidad.setDireccionMac(dominio.getDireccionMac());
        entidad.setEstado(dominio.getEstado());
        entidad.setObservacion(dominio.getObservacion());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public Equipo aDominio(EquipoEntity entidad) {
        Equipo dominio = new Equipo();
        dominio.setId(entidad.getId());
        dominio.aplicarDatos(
                entidad.getLaboratorioId(),
                entidad.getTipoEquipoId(),
                entidad.getCodigoInventario(),
                entidad.getNumeroSerie(),
                entidad.getMarca(),
                entidad.getModelo(),
                entidad.getProcesador(),
                entidad.getMemoriaRam(),
                entidad.getAlmacenamiento(),
                entidad.getDireccionIp(),
                entidad.getDireccionMac(),
                entidad.getObservacion()
        );
        dominio.setEstado(entidad.getEstado());
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
