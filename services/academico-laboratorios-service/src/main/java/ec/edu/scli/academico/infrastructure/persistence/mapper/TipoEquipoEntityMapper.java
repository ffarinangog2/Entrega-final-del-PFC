package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.TipoEquipo;
import ec.edu.scli.academico.infrastructure.persistence.entity.TipoEquipoEntity;
import org.springframework.stereotype.Component;

@Component
public class TipoEquipoEntityMapper {

    public TipoEquipoEntity aEntidad(TipoEquipo dominio) {
        TipoEquipoEntity entidad = new TipoEquipoEntity();
        entidad.setId(dominio.getId());
        entidad.setCodigo(dominio.getCodigo());
        entidad.setNombre(dominio.getNombre());
        entidad.setDescripcion(dominio.getDescripcion());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public TipoEquipo aDominio(TipoEquipoEntity entidad) {
        TipoEquipo dominio = new TipoEquipo();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(entidad.getCodigo(), entidad.getNombre(), entidad.getDescripcion());
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
