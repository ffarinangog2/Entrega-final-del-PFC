package ec.edu.scli.usuarios.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class HmacIdentificacionService {

    private static final String ALGORITMO = "HmacSHA256";

    private final SecretKeySpec claveHmac;

    public HmacIdentificacionService(@Value("${app.security.data-hash-key}") String claveBase64) {
        byte[] claveBytes = Base64.getDecoder().decode(claveBase64);
        this.claveHmac = new SecretKeySpec(claveBytes, ALGORITMO);
    }

    public String calcularHash(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(claveHmac);
            byte[] resultado = mac.doFinal(valor.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resultado);
        } catch (Exception excepcion) {
            throw new IllegalStateException("Error al calcular el hash de búsqueda", excepcion);
        }
    }
}