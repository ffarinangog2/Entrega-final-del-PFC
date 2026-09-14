package ec.edu.scli.usuarios.infrastructure.persistence.converter;

import ec.edu.scli.usuarios.infrastructure.security.CifradoAesGcmService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Converter
@Component
public class CampoCifradoConverter implements AttributeConverter<String, String> {

    private final CifradoAesGcmService cifradoService;

    public CampoCifradoConverter(CifradoAesGcmService cifradoService) {
        this.cifradoService = cifradoService;
    }

    @Override
    public String convertToDatabaseColumn(String atributo) {
        return cifradoService.cifrar(atributo);
    }

    @Override
    public String convertToEntityAttribute(String dato) {
        if (!esCifradoAesGcm(dato)) {
            return dato;
        }
        return cifradoService.descifrar(dato);
    }

    private boolean esCifradoAesGcm(String dato) {
        if (dato == null) {
            return false;
        }
        try {
            // 12 bytes de IV + al menos 16 bytes del tag GCM.
            return Base64.getDecoder().decode(dato).length >= 28;
        } catch (IllegalArgumentException excepcion) {
            return false;
        }
    }
}
