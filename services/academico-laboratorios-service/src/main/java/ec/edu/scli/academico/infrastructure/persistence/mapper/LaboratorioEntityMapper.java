package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.infrastructure.persistence.entity.LaboratorioEntity;
import org.springframework.stereotype.Component;

@Component
public class LaboratorioEntityMapper {

    public LaboratorioEntity aEntidad(Laboratorio dominio) {
        LaboratorioEntity entidad = new LaboratorioEntity();
        entidad.setId(dominio.getId());
        entidad.setPisoId(dominio.getPisoId());
        entidad.setCodigo(dominio.getCodigo());
        entidad.setNombre(dominio.getNombre());
        entidad.setCapacidad(dominio.getCapacidad());
        entidad.setDescripcion(dominio.getDescripcion());
        entidad.setEstado(dominio.getEstado());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public Laboratorio aDominio(LaboratorioEntity entidad) {
        Laboratorio dominio = new Laboratorio();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(
                entidad.getPisoId(),
                entidad.getCodigo(),
                entidad.getNombre(),
                entidad.getCapacidad(),
                entidad.getDescripcion()
        );
        dominio.setEstado(entidad.getEstado());
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
