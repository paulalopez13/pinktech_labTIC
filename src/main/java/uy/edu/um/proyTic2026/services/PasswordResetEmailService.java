package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Async
    public void send(String recipient, String from, String resetLink) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            logger.warn("Password reset email could not be sent: no mail sender is configured.");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Recuperar contraseña - Uruguay Mnatural");
        message.setText("Recibimos una solicitud para cambiar la contraseña de tu cuenta.\n\n"
                + "Abrí este enlace dentro de los próximos 20 minutos:\n" + resetLink + "\n\n"
                + "Si no pediste este cambio, podés ignorar este correo.");
        try {
            sender.send(message);
        } catch (Exception error) {
            logger.warn("Password reset email delivery failed: {}", error.getClass().getSimpleName());
        }
    }
}
