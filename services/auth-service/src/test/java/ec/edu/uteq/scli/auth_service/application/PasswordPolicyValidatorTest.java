package ec.edu.uteq.scli.auth_service.application;

import ec.edu.uteq.scli.auth_service.application.service.PasswordPolicyValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyValidatorTest {
    private final PasswordPolicyValidator validator=new PasswordPolicyValidator();
    @Test void aceptaPasswordValida(){assertDoesNotThrow(()->validator.validate("ClaveSegura1!"));}
    @Test void rechazaMenosDeDoce(){reject("Corta1!a");}
    @Test void rechazaMasDeSesentaYCuatro(){reject("Aa1!"+"x".repeat(61));}
    @Test void rechazaMasDeSetentaYDosBytesUtf8(){reject("Á".repeat(60)+"Aa1!");}
    @Test void rechazaSinMayuscula(){reject("clavesegura1!");}
    @Test void rechazaSinMinuscula(){reject("CLAVESEGURA1!");}
    @Test void rechazaSinNumero(){reject("ClaveSegura!!");}
    @Test void rechazaSinEspecial(){reject("ClaveSegura123");}
    @Test void rechazaWhitespace(){reject("Clave Segura1!");}
    private void reject(String value){assertThrows(IllegalArgumentException.class,()->validator.validate(value));}
}
