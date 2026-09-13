package ec.edu.scli.usuarios.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CifradoAesGcmService {

    private static final String TRANSFORMACION = "AES/GCM/NoPadding";
    private static final int TAMANIO_IV_BYTES = 12;
    private static final int TAMANIO_TAG_BITS = 128;

    private final SecretKeySpec claveSecreta;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    public CifradoAesGcmService(@Value("${app.security.data-encryption-key}") String claveBase64) {
        byte[] claveBytes = Base64.getDecoder().decode(claveBase64);
        if (claveBytes.length != 32) {
            throw new IllegalStateException(
                    "DATA_ENCRYPTION_KEY debe representar 32 bytes (AES-256) en Base64");
        }
        this.claveSecreta = new SecretKeySpec(claveBytes, "AES");
    }

    public String cifrar(String textoPlano) {
        if (textoPlano == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAMANIO_IV_BYTES];
            generadorAleatorio.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, claveSecreta, new GCMParameterSpec(TAMANIO_TAG_BITS, iv));

            byte[] textoCifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, resultado, iv.length, textoCifrado.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception excepcion) {
            throw new IllegalStateException("Error al cifrar el dato", excepcion);
        }
    }

    public String descifrar(String textoCifradoBase64) {
        if (textoCifradoBase64 == null) {
            return null;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifradoBase64);

            byte[] iv = new byte[TAMANIO_IV_BYTES];
            System.arraycopy(datos, 0, iv, 0, TAMANIO_IV_BYTES);

            byte[] textoCifrado = new byte[datos.length - TAMANIO_IV_BYTES];
            System.arraycopy(datos, TAMANIO_IV_BYTES, textoCifrado, 0, textoCifrado.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, claveSecreta, new GCMParameterSpec(TAMANIO_TAG_BITS, iv));

            return new String(cipher.doFinal(textoCifrado), StandardCharsets.UTF_8);
        } catch (Exception excepcion) {
            throw new IllegalStateException(
                    "Error al descifrar el dato (clave incorrecta o dato corrupto)", excepcion);
        }
    }
}