package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.Carrera;
import ec.edu.scli.academico.infrastructure.persistence.entity.CarreraEntity;
import org.springframework.stereotype.Component;

@Component
public class CarreraEntityMapper {

    public CarreraEntity aEntidad(Carrera dominio) {
        CarreraEntity entidad = new CarreraEntity();
        entidad.setId(dominio.getId());
        entidad.setFacultadId(dominio.getFacultadId());
        entidad.setCodigo(dominio.getCodigo());
        entidad.setNombre(dominio.getNombre());
        entidad.setDescripcion(dominio.getDescripcion());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public Carrera aDominio(CarreraEntity entidad) {
        Carrera dominio = new Carrera();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(
                entidad.getFacultadId(),
                entidad.getCodigo(),
                entidad.getNombre(),
                entidad.getDescripcion()
        );
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
