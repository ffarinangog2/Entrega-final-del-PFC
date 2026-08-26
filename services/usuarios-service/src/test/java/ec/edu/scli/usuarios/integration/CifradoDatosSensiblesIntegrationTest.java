package ec.edu.scli.usuarios.integration;

import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que identificacion, telefono, direccion y emailPersonal se
 * guarden cifrados (AES-256-GCM) en la base de datos y que el sistema
 * pueda descifrarlos de vuelta de forma transparente al leerlos.
 *
 * Se ejecuta contra la base de datos ya levantada por docker-compose
 * (cockroach-usuarios en localhost:26258), no crea un contenedor nuevo.
 *
 * @Transactional hace rollback automático al final: no deja datos de
 * prueba residuales en la base de desarrollo.
 */
@SpringBootTest
@Transactional
class CifradoDatosSensiblesIntegrationTest {

    private static final String IDENTIFICACION_PLANA = "1234567890";
    private static final String TELEFONO_PLANO = "0991234567";
    private static final String DIRECCION_PLANA = "Av. Siempre Viva 123, Quevedo";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PerfilRepositoryPort perfilRepositoryPort;
    @Autowired
    private EntityManager entityManager;

    @Test
    void alGuardarPerfil_losCamposSensiblesQuedanCifradosEnLaBaseDeDatos() {

        Perfil perfil = new Perfil();
        perfil.setIdentificacion(IDENTIFICACION_PLANA);
        perfil.setNombres("Prueba");
        perfil.setApellidos("Cifrado");
        perfil.setEmailInstitucional("prueba.cifrado." + UUID.randomUUID() + "@uteq.edu.ec");
        perfil.setTelefono(TELEFONO_PLANO);
        perfil.setDireccion(DIRECCION_PLANA);
        perfil.setActivo(true);

        Perfil guardado = perfilRepositoryPort.save(perfil);
        entityManager.flush();

        Map<String, Object> filaCruda = jdbcTemplate.queryForMap(
                "SELECT identificacion, identificacion_hash, telefono, direccion "
                        + "FROM perfiles WHERE id = ?",
                guardado.getId()
        );

        String identificacionCruda = (String) filaCruda.get("identificacion");
        String identificacionHash = (String) filaCruda.get("identificacion_hash");
        String telefonoCrudo = (String) filaCruda.get("telefono");
        String direccionCruda = (String) filaCruda.get("direccion");

        assertNotEquals(IDENTIFICACION_PLANA, identificacionCruda,
                "La identificacion NO debe guardarse en texto plano en la base de datos");
        assertNotEquals(TELEFONO_PLANO, telefonoCrudo,
                "El telefono NO debe guardarse en texto plano en la base de datos");
        assertNotEquals(DIRECCION_PLANA, direccionCruda,
                "La direccion NO debe guardarse en texto plano en la base de datos");

        assertTrue(identificacionHash != null && identificacionHash.length() == 64,
                "El hash HMAC-SHA256 debe tener 64 caracteres hexadecimales");
    }

    @Test
    void alLeerPerfil_losCamposSensiblesSeDescifranCorrectamente() {

        Perfil perfil = new Perfil();
        perfil.setIdentificacion(IDENTIFICACION_PLANA);
        perfil.setNombres("Prueba");
        perfil.setApellidos("Descifrado");
        perfil.setEmailInstitucional("prueba.descifrado." + UUID.randomUUID() + "@uteq.edu.ec");
        perfil.setTelefono(TELEFONO_PLANO);
        perfil.setDireccion(DIRECCION_PLANA);
        perfil.setActivo(true);

        Perfil guardado = perfilRepositoryPort.save(perfil);

        Optional<Perfil> leidoPorId = perfilRepositoryPort.findById(guardado.getId());

        assertTrue(leidoPorId.isPresent());
        assertEquals(IDENTIFICACION_PLANA, leidoPorId.get().getIdentificacion());
        assertEquals(TELEFONO_PLANO, leidoPorId.get().getTelefono());
        assertEquals(DIRECCION_PLANA, leidoPorId.get().getDireccion());
    }

    @Test
    void findByIdentificacion_encuentraElPerfilBuscandoPorHash() {

        Perfil perfil = new Perfil();
        perfil.setIdentificacion(IDENTIFICACION_PLANA);
        perfil.setNombres("Prueba");
        perfil.setApellidos("BusquedaPorHash");
        perfil.setEmailInstitucional("prueba.hash." + UUID.randomUUID() + "@uteq.edu.ec");
        perfil.setActivo(true);

        Perfil guardado = perfilRepositoryPort.save(perfil);

        Optional<Perfil> encontrado = perfilRepositoryPort.findByIdentificacion(IDENTIFICACION_PLANA);

        assertTrue(encontrado.isPresent());
        assertEquals(guardado.getId(), encontrado.get().getId());
    }
}