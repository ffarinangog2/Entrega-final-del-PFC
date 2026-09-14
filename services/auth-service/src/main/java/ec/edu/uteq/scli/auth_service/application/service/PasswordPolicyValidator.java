package ec.edu.uteq.scli.auth_service.application.service;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
public class PasswordPolicyValidator {
    public void validate(String password) {
        if (password == null || password.isEmpty()) fail();
        if (password.length() < 12 || password.length() > 64) fail();
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) fail();
        if (password.chars().anyMatch(Character::isWhitespace)) fail();
        if (password.chars().noneMatch(Character::isUpperCase)) fail();
        if (password.chars().noneMatch(Character::isLowerCase)) fail();
        if (password.chars().noneMatch(Character::isDigit)) fail();
        if (password.codePoints().noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) fail();
    }
    private void fail() { throw new IllegalArgumentException("La contraseña no cumple la política de seguridad"); }
}
