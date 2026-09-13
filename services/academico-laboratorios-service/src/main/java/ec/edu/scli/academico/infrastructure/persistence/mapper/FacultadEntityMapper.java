package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.Facultad;
import ec.edu.scli.academico.infrastructure.persistence.entity.FacultadEntity;
import org.springframework.stereotype.Component;

@Component
public class FacultadEntityMapper {

    public FacultadEntity aEntidad(Facultad dominio) {
        FacultadEntity entidad = new FacultadEntity();
        entidad.setId(dominio.getId());
        entidad.setCodigo(dominio.getCodigo());
        entidad.setNombre(dominio.getNombre());
        entidad.setDescripcion(dominio.getDescripcion());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public Facultad aDominio(FacultadEntity entidad) {
        Facultad dominio = new Facultad();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(entidad.getCodigo(), entidad.getNombre(), entidad.getDescripcion());
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
