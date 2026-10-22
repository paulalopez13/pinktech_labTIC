package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public void send(String recipient, String from, String resetLink) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            logger.warn("Password reset email could not be sent: no mail sender is configured.");
            throw new IllegalStateException("El correo de recuperación no está configurado.");
        }
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject("Recuperar contraseña - Uruguay Mnatural");
            helper.setText("Recibimos una solicitud para cambiar la contraseña de tu cuenta.\n\n"
                    + "Abrí este enlace dentro de los próximos 20 minutos:\n" + resetLink + "\n\n"
                    + "Si no pediste este cambio, podés ignorar este correo.", """
                    <!doctype html><html lang="es"><body style="margin:0;padding:32px 12px;background:#edf4f1;font-family:Arial,Helvetica,sans-serif;color:#23433d">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;margin:auto;background:#fff;border-radius:18px;overflow:hidden">
                    <tr><td style="background:#0d7180;padding:24px 32px;color:#fff;font-size:24px;font-weight:bold">🌿 Uruguay Mnatural</td></tr>
                    <tr><td><img src="cid:heroImage" alt="Isla Gorriti, Uruguay" width="600" style="display:block;width:100%%;height:190px;object-fit:cover"></td></tr>
                    <tr><td style="padding:32px;font-size:16px;line-height:1.7"><h2 style="margin-top:0;color:#164e49">Recuperá tu contraseña</h2><p>Recibimos una solicitud para cambiar la contraseña de tu cuenta.</p><p>El enlace es válido durante 20 minutos.</p>
                    <p style="text-align:center;padding:12px 0"><a href="%s" style="display:inline-block;background:#0d7180;color:#fff;text-decoration:none;font-weight:bold;padding:14px 28px;border-radius:10px">Crear nueva contraseña</a></p>
                    <p style="font-size:13px;color:#71837e">Si no pediste este cambio, podés ignorar este correo.</p></td></tr>
                    <tr><td style="background:#f5f8f6;padding:18px;text-align:center;color:#71837e;font-size:12px">Uruguay Mnatural · Turismo auténtico en Uruguay</td></tr>
                    </table></body></html>
                    """.formatted(escapeHtml(resetLink)));
            helper.addInline("heroImage", new ClassPathResource("mail-images/isla-gorriti.png"), "image/png");
            sender.send(message);
        } catch (Exception error) {
            logger.warn("Password reset email delivery failed: {}", error.getClass().getSimpleName());
            if (error instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("No se pudo enviar el correo de recuperación.", error);
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
