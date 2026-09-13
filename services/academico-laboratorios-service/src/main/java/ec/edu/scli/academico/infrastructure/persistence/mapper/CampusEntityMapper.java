package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.Campus;
import ec.edu.scli.academico.infrastructure.persistence.entity.CampusEntity;
import org.springframework.stereotype.Component;

/** Traduce entre el modelo de dominio puro y la entidad JPA de infraestructura. */
@Component
public class CampusEntityMapper {

    public CampusEntity aEntidad(Campus dominio) {
        CampusEntity entidad = new CampusEntity();
        entidad.setId(dominio.getId());
        entidad.setCodigo(dominio.getCodigo());
        entidad.setNombre(dominio.getNombre());
        entidad.setDireccion(dominio.getDireccion());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public Campus aDominio(CampusEntity entidad) {
        Campus dominio = new Campus();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(entidad.getCodigo(), entidad.getNombre(), entidad.getDireccion());
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
