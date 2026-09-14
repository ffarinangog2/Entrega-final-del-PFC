package ec.edu.uteq.scli.auth_service.infrastructure.mail;

import ec.edu.uteq.scli.auth_service.application.service.PasswordResetMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SpringPasswordResetMailService implements PasswordResetMailService {
    private final JavaMailSender sender; private final String from;
    public SpringPasswordResetMailService(JavaMailSender sender, @Value("${app.password-reset.mail-from}") String from) {
        this.sender=sender; this.from=from;
    }
    public void sendResetLink(String email, String link) {
        SimpleMailMessage message=new SimpleMailMessage(); message.setFrom(from); message.setTo(email);
        message.setSubject("Recuperación de contraseña SCLI");
        message.setText("Se solicitó restablecer su contraseña. El enlace vence pronto y solo puede utilizarse una vez:\n\n"+link+"\n\nSi no realizó la solicitud, ignore este mensaje.");
        sender.send(message);
    }
}
