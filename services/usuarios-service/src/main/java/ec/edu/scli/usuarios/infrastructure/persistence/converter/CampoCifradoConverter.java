package ec.edu.scli.usuarios.infrastructure.persistence.converter;

import ec.edu.scli.usuarios.infrastructure.security.CifradoAesGcmService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

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
        return cifradoService.descifrar(dato);
    }
}