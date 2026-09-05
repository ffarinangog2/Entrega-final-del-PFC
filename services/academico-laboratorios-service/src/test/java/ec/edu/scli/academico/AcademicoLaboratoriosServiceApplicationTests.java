package ec.edu.scli.academico;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto de Spring Boot completo levanta sin errores
 * (todas las entidades, repositorios, servicios y controladores se
 * conectan correctamente entre sí).
 *
 * Requiere una base de datos disponible según application.yml
 * (usar el perfil "test" con una base local, o correr con Docker
 * Compose levantado).
 */
@SpringBootTest
@ActiveProfiles("test")
class AcademicoLaboratoriosServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
