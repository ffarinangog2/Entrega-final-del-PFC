package ec.edu.scli.usuarios.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

@Component
public class PerfilPersistenceMapper {

    public ec.edu.scli.usuarios.domain.model.Perfil toDomain(
            ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil entity
    ) {
        if (entity == null) {
            return null;
        }

        ec.edu.scli.usuarios.domain.model.Perfil domain =
                new ec.edu.scli.usuarios.domain.model.Perfil();

        domain.setId(entity.getId());
        domain.setIdentificacion(entity.getIdentificacion());
        domain.setNombres(entity.getNombres());
        domain.setApellidos(entity.getApellidos());
        domain.setEmailInstitucional(entity.getEmailInstitucional());
        domain.setEmailPersonal(entity.getEmailPersonal());
        domain.setTelefono(entity.getTelefono());
        domain.setDireccion(entity.getDireccion());
        domain.setFechaNacimiento(entity.getFechaNacimiento());
        domain.setFotoUrl(entity.getFotoUrl());
        domain.setActivo(entity.getActivo());
        domain.setCreadoEn(entity.getCreadoEn());
        domain.setActualizadoEn(entity.getActualizadoEn());

        return domain;
    }

    public ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil toEntity(
            ec.edu.scli.usuarios.domain.model.Perfil domain
    ) {
        if (domain == null) {
            return null;
        }

        ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil();

        entity.setId(domain.getId());
        entity.setIdentificacion(domain.getIdentificacion());
        entity.setNombres(domain.getNombres());
        entity.setApellidos(domain.getApellidos());
        entity.setEmailInstitucional(domain.getEmailInstitucional());
        entity.setEmailPersonal(domain.getEmailPersonal());
        entity.setTelefono(domain.getTelefono());
        entity.setDireccion(domain.getDireccion());
        entity.setFechaNacimiento(domain.getFechaNacimiento());
        entity.setFotoUrl(domain.getFotoUrl());
        entity.setActivo(domain.getActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());

        return entity;
    }
}
