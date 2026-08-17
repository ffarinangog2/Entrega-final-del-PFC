package ec.edu.scli.usuarios.infrastructure.persistence.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilRelacionadoPersistenceMapperTest {

    private PerfilRelacionadoPersistenceMapper mapper;
    private ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil perfilEntity;
    private ec.edu.scli.usuarios.domain.model.Perfil perfilDomain;

    @BeforeEach
    void setUp() {
        mapper = new PerfilRelacionadoPersistenceMapper(new PerfilPersistenceMapper());

        UUID perfilId = UUID.randomUUID();

        perfilEntity = new ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil();
        perfilEntity.setId(perfilId);
        perfilEntity.setIdentificacion("0102030405");
        perfilEntity.setActivo(true);

        perfilDomain = new ec.edu.scli.usuarios.domain.model.Perfil();
        perfilDomain.setId(perfilId);
        perfilDomain.setIdentificacion("0102030405");
        perfilDomain.setActivo(true);
    }

    // ---------------------------------------------------------------
    // Docente
    // ---------------------------------------------------------------

    @Test
    void docente_toDomain_deberiaMapearTodosLosCampos() {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente();
        entity.setId(UUID.randomUUID());
        entity.setPerfil(perfilEntity);
        entity.setCodigoDocente("DOC-001");
        entity.setTituloAcademico("Magister");
        entity.setDepartamento("Sistemas");
        entity.setTipoContrato("Tiempo completo");
        entity.setDedicacion("40h");
        entity.setActivo(true);
        entity.setCreadoEn(OffsetDateTime.now());
        entity.setActualizadoEn(OffsetDateTime.now());

        ec.edu.scli.usuarios.domain.model.Docente domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getPerfil().getId()).isEqualTo(perfilEntity.getId());
        assertThat(domain.getCodigoDocente()).isEqualTo("DOC-001");
        assertThat(domain.getTituloAcademico()).isEqualTo("Magister");
        assertThat(domain.getDepartamento()).isEqualTo("Sistemas");
        assertThat(domain.getTipoContrato()).isEqualTo("Tiempo completo");
        assertThat(domain.getDedicacion()).isEqualTo("40h");
        assertThat(domain.getActivo()).isTrue();
    }

    @Test
    void docente_toEntity_deberiaMapearTodosLosCampos() {
        ec.edu.scli.usuarios.domain.model.Docente domain =
                new ec.edu.scli.usuarios.domain.model.Docente();
        domain.setId(UUID.randomUUID());
        domain.setPerfil(perfilDomain);
        domain.setCodigoDocente("DOC-001");
        domain.setTituloAcademico("Magister");
        domain.setDepartamento("Sistemas");
        domain.setTipoContrato("Tiempo completo");
        domain.setDedicacion("40h");
        domain.setActivo(true);
        domain.setCreadoEn(OffsetDateTime.now());
        domain.setActualizadoEn(OffsetDateTime.now());

        ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente entity =
                mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getPerfil().getId()).isEqualTo(perfilDomain.getId());
        assertThat(entity.getCodigoDocente()).isEqualTo("DOC-001");
        assertThat(entity.getTituloAcademico()).isEqualTo("Magister");
        assertThat(entity.getDepartamento()).isEqualTo("Sistemas");
        assertThat(entity.getTipoContrato()).isEqualTo("Tiempo completo");
        assertThat(entity.getDedicacion()).isEqualTo("40h");
        assertThat(entity.getActivo()).isTrue();
    }

    // ---------------------------------------------------------------
    // Estudiante
    // ---------------------------------------------------------------

    @Test
    void estudiante_toDomain_deberiaMapearTodosLosCampos() {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante();
        entity.setId(UUID.randomUUID());
        entity.setPerfil(perfilEntity);
        entity.setMatricula("MAT-001");
        entity.setCarreraId(UUID.randomUUID());
        entity.setSemestre(3);
        entity.setActivo(true);
        entity.setCreadoEn(OffsetDateTime.now());
        entity.setActualizadoEn(OffsetDateTime.now());

        ec.edu.scli.usuarios.domain.model.Estudiante domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getPerfil().getId()).isEqualTo(perfilEntity.getId());
        assertThat(domain.getMatricula()).isEqualTo("MAT-001");
        assertThat(domain.getCarreraId()).isEqualTo(entity.getCarreraId());
        assertThat(domain.getSemestre()).isEqualTo(3);
        assertThat(domain.getActivo()).isTrue();
    }

    @Test
    void estudiante_toEntity_deberiaMapearTodosLosCampos() {
        ec.edu.scli.usuarios.domain.model.Estudiante domain =
                new ec.edu.scli.usuarios.domain.model.Estudiante();
        domain.setId(UUID.randomUUID());
        domain.setPerfil(perfilDomain);
        domain.setMatricula("MAT-001");
        domain.setCarreraId(UUID.randomUUID());
        domain.setSemestre(3);
        domain.setActivo(true);
        domain.setCreadoEn(OffsetDateTime.now());
        domain.setActualizadoEn(OffsetDateTime.now());

        ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante entity =
                mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getPerfil().getId()).isEqualTo(perfilDomain.getId());
        assertThat(entity.getMatricula()).isEqualTo("MAT-001");
        assertThat(entity.getCarreraId()).isEqualTo(domain.getCarreraId());
        assertThat(entity.getSemestre()).isEqualTo(3);
        assertThat(entity.getActivo()).isTrue();
    }

    // ---------------------------------------------------------------
    // Tecnico
    // ---------------------------------------------------------------

    @Test
    void tecnico_toDomain_deberiaMapearTodosLosCampos() {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico();
        entity.setId(UUID.randomUUID());
        entity.setPerfil(perfilEntity);
        entity.setCodigoTecnico("TEC-001");
        entity.setEspecialidad("Redes");
        entity.setActivo(true);
        entity.setCreadoEn(OffsetDateTime.now());
        entity.setActualizadoEn(OffsetDateTime.now());

        ec.edu.scli.usuarios.domain.model.Tecnico domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getPerfil().getId()).isEqualTo(perfilEntity.getId());
        assertThat(domain.getCodigoTecnico()).isEqualTo("TEC-001");
        assertThat(domain.getEspecialidad()).isEqualTo("Redes");
        assertThat(domain.getActivo()).isTrue();
    }

    @Test
    void tecnico_toEntity_deberiaMapearTodosLosCampos() {
        ec.edu.scli.usuarios.domain.model.Tecnico domain =
                new ec.edu.scli.usuarios.domain.model.Tecnico();
        domain.setId(UUID.randomUUID());
        domain.setPerfil(perfilDomain);
        domain.setCodigoTecnico("TEC-001");
        domain.setEspecialidad("Redes");
        domain.setActivo(true);
        domain.setCreadoEn(OffsetDateTime.now());
        domain.setActualizadoEn(OffsetDateTime.now());

        ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico entity =
                mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getPerfil().getId()).isEqualTo(perfilDomain.getId());
        assertThat(entity.getCodigoTecnico()).isEqualTo("TEC-001");
        assertThat(entity.getEspecialidad()).isEqualTo("Redes");
        assertThat(entity.getActivo()).isTrue();
    }

    // ---------------------------------------------------------------
    // Administrador
    // ---------------------------------------------------------------

    @Test
    void administrador_toDomain_deberiaMapearTodosLosCampos() {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador();
        entity.setId(UUID.randomUUID());
        entity.setPerfil(perfilEntity);
        entity.setCodigoAdministrador("ADM-001");
        entity.setCargo("Coordinador");
        entity.setPisoId(UUID.randomUUID());
        entity.setActivo(true);
        entity.setCreadoEn(OffsetDateTime.now());
        entity.setActualizadoEn(OffsetDateTime.now());

        ec.edu.scli.usuarios.domain.model.Administrador domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getPerfil().getId()).isEqualTo(perfilEntity.getId());
        assertThat(domain.getCodigoAdministrador()).isEqualTo("ADM-001");
        assertThat(domain.getCargo()).isEqualTo("Coordinador");
        assertThat(domain.getPisoId()).isEqualTo(entity.getPisoId());
        assertThat(domain.getActivo()).isTrue();
    }

    @Test
    void administrador_toEntity_deberiaMapearTodosLosCampos() {
        ec.edu.scli.usuarios.domain.model.Administrador domain =
                new ec.edu.scli.usuarios.domain.model.Administrador();
        domain.setId(UUID.randomUUID());
        domain.setPerfil(perfilDomain);
        domain.setCodigoAdministrador("ADM-001");
        domain.setCargo("Coordinador");
        domain.setPisoId(UUID.randomUUID());
        domain.setActivo(true);
        domain.setCreadoEn(OffsetDateTime.now());
        domain.setActualizadoEn(OffsetDateTime.now());

        ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador entity =
                mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getPerfil().getId()).isEqualTo(perfilDomain.getId());
        assertThat(entity.getCodigoAdministrador()).isEqualTo("ADM-001");
        assertThat(entity.getCargo()).isEqualTo("Coordinador");
        assertThat(entity.getPisoId()).isEqualTo(domain.getPisoId());
        assertThat(entity.getActivo()).isTrue();
    }
}
