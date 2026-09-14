package ec.edu.scli.academico.infrastructure.bootstrap;

import ec.edu.scli.academico.infrastructure.persistence.entity.CampusEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
class AssignedUuidPersistenceProbeTest {
    @Autowired
    private EntityManager entityManager;

    @Test
    void conservaUuidAsignadoAlPersistir() {
        CampusEntity campus = new CampusEntity();
        campus.setId(InitialAcademicDataBootstrap.CAMPUS_ID);
        campus.setCodigo("PROBE");
        campus.setNombre("Probe");
        campus.setActivo(true);

        entityManager.persist(campus);
        entityManager.flush();

        assertEquals(InitialAcademicDataBootstrap.CAMPUS_ID, campus.getId());
    }

    @Test
    void generaUuidCuandoLaEntidadNoTraeUnoAsignado() {
        CampusEntity campus = new CampusEntity();
        campus.setCodigo("GENERATED");
        campus.setNombre("Generated");
        campus.setActivo(true);

        entityManager.persist(campus);
        entityManager.flush();

        assertNotNull(campus.getId());
    }
}
