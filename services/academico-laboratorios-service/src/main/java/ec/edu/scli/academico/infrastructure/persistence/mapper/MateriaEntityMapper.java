package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.Materia;
import ec.edu.scli.academico.infrastructure.persistence.entity.MateriaEntity;
import org.springframework.stereotype.Component;

@Component
public class MateriaEntityMapper {

    public MateriaEntity aEntidad(Materia dominio) {
        MateriaEntity entidad = new MateriaEntity();
        entidad.setId(dominio.getId());
        entidad.setCarreraId(dominio.getCarreraId());
        entidad.setCodigo(dominio.getCodigo());
        entidad.setNombre(dominio.getNombre());
        entidad.setNumeroHoras(dominio.getNumeroHoras());
        entidad.setNivel(dominio.getNivel());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public Materia aDominio(MateriaEntity entidad) {
        Materia dominio = new Materia();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(
                entidad.getCarreraId(),
                entidad.getCodigo(),
                entidad.getNombre(),
                entidad.getNumeroHoras()
        );
        dominio.setActivo(entidad.isActivo());
        dominio.setNivel(entidad.getNivel());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
