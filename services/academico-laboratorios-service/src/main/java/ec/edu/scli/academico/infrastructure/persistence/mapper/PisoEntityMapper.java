package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.Piso;
import ec.edu.scli.academico.infrastructure.persistence.entity.PisoEntity;
import org.springframework.stereotype.Component;

@Component
public class PisoEntityMapper {

    public PisoEntity aEntidad(Piso dominio) {
        PisoEntity entidad = new PisoEntity();
        entidad.setId(dominio.getId());
        entidad.setBloqueId(dominio.getBloqueId());
        entidad.setNumero(dominio.getNumero());
        entidad.setDescripcion(dominio.getDescripcion());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public Piso aDominio(PisoEntity entidad) {
        Piso dominio = new Piso();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(entidad.getBloqueId(), entidad.getNumero(), entidad.getDescripcion());
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
