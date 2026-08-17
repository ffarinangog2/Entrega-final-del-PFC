package ec.edu.scli.usuarios.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

@Component
public class PerfilRelacionadoPersistenceMapper {

    private final PerfilPersistenceMapper perfilMapper;

    public PerfilRelacionadoPersistenceMapper(PerfilPersistenceMapper perfilMapper) {
        this.perfilMapper = perfilMapper;
    }

    public ec.edu.scli.usuarios.domain.model.Docente toDomain(
            ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente entity
    ) {
        ec.edu.scli.usuarios.domain.model.Docente domain =
                new ec.edu.scli.usuarios.domain.model.Docente();
        domain.setId(entity.getId());
        domain.setPerfil(perfilMapper.toDomain(entity.getPerfil()));
        domain.setCodigoDocente(entity.getCodigoDocente());
        domain.setTituloAcademico(entity.getTituloAcademico());
        domain.setDepartamento(entity.getDepartamento());
        domain.setTipoContrato(entity.getTipoContrato());
        domain.setDedicacion(entity.getDedicacion());
        domain.setActivo(entity.getActivo());
        domain.setCreadoEn(entity.getCreadoEn());
        domain.setActualizadoEn(entity.getActualizadoEn());
        return domain;
    }

    public ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente toEntity(
            ec.edu.scli.usuarios.domain.model.Docente domain
    ) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente();
        entity.setId(domain.getId());
        entity.setPerfil(perfilMapper.toEntity(domain.getPerfil()));
        entity.setCodigoDocente(domain.getCodigoDocente());
        entity.setTituloAcademico(domain.getTituloAcademico());
        entity.setDepartamento(domain.getDepartamento());
        entity.setTipoContrato(domain.getTipoContrato());
        entity.setDedicacion(domain.getDedicacion());
        entity.setActivo(domain.getActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }

    public ec.edu.scli.usuarios.domain.model.Estudiante toDomain(
            ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante entity
    ) {
        ec.edu.scli.usuarios.domain.model.Estudiante domain =
                new ec.edu.scli.usuarios.domain.model.Estudiante();
        domain.setId(entity.getId());
        domain.setPerfil(perfilMapper.toDomain(entity.getPerfil()));
        domain.setMatricula(entity.getMatricula());
        domain.setCarreraId(entity.getCarreraId());
        domain.setSemestre(entity.getSemestre());
        domain.setActivo(entity.getActivo());
        domain.setCreadoEn(entity.getCreadoEn());
        domain.setActualizadoEn(entity.getActualizadoEn());
        return domain;
    }

    public ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante toEntity(
            ec.edu.scli.usuarios.domain.model.Estudiante domain
    ) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante();
        entity.setId(domain.getId());
        entity.setPerfil(perfilMapper.toEntity(domain.getPerfil()));
        entity.setMatricula(domain.getMatricula());
        entity.setCarreraId(domain.getCarreraId());
        entity.setSemestre(domain.getSemestre());
        entity.setActivo(domain.getActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }

    public ec.edu.scli.usuarios.domain.model.Tecnico toDomain(
            ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico entity
    ) {
        ec.edu.scli.usuarios.domain.model.Tecnico domain =
                new ec.edu.scli.usuarios.domain.model.Tecnico();
        domain.setId(entity.getId());
        domain.setPerfil(perfilMapper.toDomain(entity.getPerfil()));
        domain.setCodigoTecnico(entity.getCodigoTecnico());
        domain.setEspecialidad(entity.getEspecialidad());
        domain.setActivo(entity.getActivo());
        domain.setCreadoEn(entity.getCreadoEn());
        domain.setActualizadoEn(entity.getActualizadoEn());
        return domain;
    }

    public ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico toEntity(
            ec.edu.scli.usuarios.domain.model.Tecnico domain
    ) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico();
        entity.setId(domain.getId());
        entity.setPerfil(perfilMapper.toEntity(domain.getPerfil()));
        entity.setCodigoTecnico(domain.getCodigoTecnico());
        entity.setEspecialidad(domain.getEspecialidad());
        entity.setActivo(domain.getActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }

    public ec.edu.scli.usuarios.domain.model.Administrador toDomain(
            ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador entity
    ) {
        ec.edu.scli.usuarios.domain.model.Administrador domain =
                new ec.edu.scli.usuarios.domain.model.Administrador();
        domain.setId(entity.getId());
        domain.setPerfil(perfilMapper.toDomain(entity.getPerfil()));
        domain.setCodigoAdministrador(entity.getCodigoAdministrador());
        domain.setCargo(entity.getCargo());
        domain.setPisoId(entity.getPisoId());
        domain.setActivo(entity.getActivo());
        domain.setCreadoEn(entity.getCreadoEn());
        domain.setActualizadoEn(entity.getActualizadoEn());
        return domain;
    }

    public ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador toEntity(
            ec.edu.scli.usuarios.domain.model.Administrador domain
    ) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador();
        entity.setId(domain.getId());
        entity.setPerfil(perfilMapper.toEntity(domain.getPerfil()));
        entity.setCodigoAdministrador(domain.getCodigoAdministrador());
        entity.setCargo(domain.getCargo());
        entity.setPisoId(domain.getPisoId());
        entity.setActivo(domain.getActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }
}
