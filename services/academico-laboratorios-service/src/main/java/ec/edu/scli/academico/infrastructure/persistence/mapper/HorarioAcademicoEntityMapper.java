package ec.edu.scli.academico.infrastructure.persistence.mapper;

import ec.edu.scli.academico.domain.model.HorarioAcademico;
import ec.edu.scli.academico.infrastructure.persistence.entity.HorarioAcademicoEntity;
import org.springframework.stereotype.Component;

@Component
public class HorarioAcademicoEntityMapper {

    public HorarioAcademicoEntity aEntidad(HorarioAcademico dominio) {
        HorarioAcademicoEntity entidad = new HorarioAcademicoEntity();
        entidad.setId(dominio.getId());
        entidad.setMateriaId(dominio.getMateriaId());
        entidad.setPeriodoLectivoId(dominio.getPeriodoLectivoId());
        entidad.setLaboratorioId(dominio.getLaboratorioId());
        entidad.setDocenteId(dominio.getDocenteId());
        entidad.setDiaSemana(dominio.getDiaSemana());
        entidad.setHoraInicio(dominio.getHoraInicio());
        entidad.setHoraFin(dominio.getHoraFin());
        entidad.setParalelo(dominio.getParalelo());
        entidad.setActivo(dominio.isActivo());
        entidad.setCreadoEn(dominio.getCreadoEn());
        entidad.setActualizadoEn(dominio.getActualizadoEn());
        return entidad;
    }

    public HorarioAcademico aDominio(HorarioAcademicoEntity entidad) {
        HorarioAcademico dominio = HorarioAcademico.nuevo(
                entidad.getMateriaId(),
                entidad.getPeriodoLectivoId(),
                entidad.getLaboratorioId(),
                entidad.getDocenteId(),
                entidad.getDiaSemana(),
                entidad.getHoraInicio(),
                entidad.getHoraFin(),
                entidad.getParalelo()
        );
        dominio.setId(entidad.getId());
        dominio.setActivo(entidad.isActivo());
        dominio.setCreadoEn(entidad.getCreadoEn());
        dominio.setActualizadoEn(entidad.getActualizadoEn());
        return dominio;
    }
}
