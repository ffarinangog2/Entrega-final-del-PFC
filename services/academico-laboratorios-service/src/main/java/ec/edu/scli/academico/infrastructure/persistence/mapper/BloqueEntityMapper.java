package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.Bloque;
import ec.edu.scli.academico.infrastructure.persistence.entity.BloqueEntity;
import org.springframework.stereotype.Component;

@Component
public class BloqueEntityMapper {

    public BloqueEntity aEntidad(Bloque dominio) {
        BloqueEntity entidad = new BloqueEntity();
        entidad.setId(dominio.getId());
        entidad.setCampusId(dominio.getCampusId());
        entidad.setCodigo(dominio.getCodigo());
        entidad.setNombre(dominio.getNombre());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public Bloque aDominio(BloqueEntity entidad) {
        Bloque dominio = new Bloque();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(entidad.getCampusId(), entidad.getCodigo(), entidad.getNombre());
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
