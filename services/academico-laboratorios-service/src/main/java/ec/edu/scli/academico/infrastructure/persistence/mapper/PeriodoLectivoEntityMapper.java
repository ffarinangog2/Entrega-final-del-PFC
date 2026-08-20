package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.PeriodoLectivo;
import ec.edu.scli.academico.infrastructure.persistence.entity.PeriodoLectivoEntity;
import org.springframework.stereotype.Component;

@Component
public class PeriodoLectivoEntityMapper {

    public PeriodoLectivoEntity aEntidad(PeriodoLectivo dominio) {
        PeriodoLectivoEntity entidad = new PeriodoLectivoEntity();
        entidad.setId(dominio.getId());
        entidad.setCodigo(dominio.getCodigo());
        entidad.setNombre(dominio.getNombre());
        entidad.setFechaInicio(dominio.getFechaInicio());
        entidad.setFechaFin(dominio.getFechaFin());
        entidad.setEstado(dominio.getEstado());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public PeriodoLectivo aDominio(PeriodoLectivoEntity entidad) {
        PeriodoLectivo dominio = new PeriodoLectivo();
        dominio.setId(entidad.getId());
        dominio.actualizarDatos(
                entidad.getCodigo(),
                entidad.getNombre(),
                entidad.getFechaInicio(),
                entidad.getFechaFin()
        );
        dominio.setEstado(entidad.getEstado());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
