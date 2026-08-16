package ec.edu.uteq.scli.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class GenerarPasswordHashTest {

    @Test
    void generarHashAdministrador() {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        String passwordPlano = "Admin123*";

        String hash = encoder.encode(passwordPlano);

        System.out.println("HASH_GENERADO=" + hash);
    }
}