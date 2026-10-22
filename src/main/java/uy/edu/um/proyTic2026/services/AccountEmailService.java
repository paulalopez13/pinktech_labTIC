package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountEmailService {
    private static final Logger logger = LoggerFactory.getLogger(AccountEmailService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.frontend-url:http://127.0.0.1:5173}")
    private String frontendUrl;

    @Async
    public void send(String recipient, String subject, String body, String heroImage) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || fromAddress == null || fromAddress.isBlank()) {
            logger.warn("Account notification email skipped: SMTP sender or SMTP_FROM is not configured.");
            return;
        }
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, html(body));
            helper.addInline("heroImage", new ClassPathResource("mail-images/" + heroImage), "image/png");
            sender.send(message);
        } catch (Exception error) {
            String details = error.getClass().getSimpleName() + ": " + error.getMessage();
            if (error instanceof MailSendException mailError && !mailError.getFailedMessages().isEmpty()) {
                details += " | Failed message details: " + mailError.getFailedMessages().values().stream()
                        .map(failure -> failure.getClass().getSimpleName() + ": " + failure.getMessage())
                        .collect(java.util.stream.Collectors.joining("; "));
            }
            logger.warn("Account notification email delivery failed: {}", details);
        }
    }

    private String html(String body) {
        String safeBody = escape(body).replace("\n", "<br>");
        String safeUrl = escape(frontendUrl);
        return """
                <!doctype html><html lang="es"><body style="margin:0;padding:0;background:#edf4f1;font-family:Arial,Helvetica,sans-serif;color:#23433d">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#edf4f1;padding:32px 12px"><tr><td align="center">
                <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%%;background:#ffffff;border-radius:18px;overflow:hidden">
                <tr><td style="background:#0d7180;padding:24px 32px;color:#ffffff"><div style="font-size:25px;font-weight:bold;letter-spacing:.3px">🌿 Uruguay Mnatural</div><div style="font-size:13px;margin-top:6px;color:#d9f0e9">Experiencias para descubrir Uruguay</div></td></tr>
                <tr><td><img src="cid:heroImage" alt="Paisaje de Uruguay" width="600" style="display:block;width:100%%;height:190px;object-fit:cover"></td></tr>
                <tr><td style="padding:30px 34px 22px;font-size:16px;line-height:1.7">%s</td></tr>
                <tr><td align="center" style="padding:0 30px 32px"><a href="%s" style="display:inline-block;background:#0d7180;color:#ffffff;text-decoration:none;font-weight:bold;padding:14px 28px;border-radius:10px">Ir a Uruguay Mnatural</a></td></tr>
                <tr><td style="background:#f5f8f6;padding:18px 28px;text-align:center;color:#71837e;font-size:12px;line-height:1.6">Recibiste este correo por una actividad en tu cuenta.<br>Uruguay Mnatural · Turismo auténtico en Uruguay</td></tr>
                </table></td></tr></table></body></html>
                """.formatted(safeBody, safeUrl);
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public void touristRegistered(String email, String username) {
        send(email, "¡Bienvenido/a a Uruguay Mnatural!", "¡Hola " + username + "!\n\n"
                + "Tu cuenta de turista se creó correctamente. Ya podés iniciar sesión y explorar actividades en Uruguay.",
                "casa-pueblo.png");
    }

    public void providerRequested(String email, String username) {
        send(email, "Recibimos tu solicitud de prestador", "¡Hola " + username + "!\n\n"
                + "Tu solicitud para registrarte como prestador se envió correctamente. Te avisaremos cuando sea revisada.",
                "bodega-jose-ignacio.png");
    }

    public void providerApproved(String email, String username) {
        send(email, "Tu solicitud de prestador fue aprobada", "¡Hola " + username + "!\n\n"
                + "Tu solicitud fue aprobada. Ya podés iniciar sesión y acceder a las funciones para prestadores.",
                "campo-durazno.png");
    }

    public void administratorCreated(String email, String username, String assignedPassword) {
        send(email, "Tu cuenta de administrador está lista", "¡Hola " + username + "!\n\n"
                + "Se creó una cuenta de administrador para vos en Uruguay Mnatural.\n\n"
                + "Usuario: " + username + "\nContraseña asignada: " + assignedPassword + "\n\n"
                + "Ya podés iniciar sesión con tu usuario o email y la contraseña asignada.",
                "calle-suspiros.png");
    }

    public void activityPublished(String email, String activityName) {
        send(email, "Tu actividad ya está publicada", "¡Hola!\n\n"
                + "La actividad \"" + activityName + "\" se publicó correctamente en Uruguay Mnatural.\n\n"
                + "Ya pueden encontrarla y reservarla desde la sección Explorar actividades.",
                "bodega-jose-ignacio.png");
    }

    public void reservationConfirmed(String email, String contactName, String activityName,
                                    String city, String location, LocalDateTime activityStart,
                                    int participantCount, BigDecimal total) {
        String date = activityStart.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm", new Locale("es", "UY")));
        String price = total.signum() == 0 ? "Gratis" : "$U " + total.stripTrailingZeros().toPlainString();
        send(email, "Tu reserva está confirmada · " + activityName,
                "¡Hola " + contactName + "!\n\n"
                        + "Tu reserva quedó confirmada.\n\n"
                        + "Actividad: " + activityName + "\n"
                        + "Fecha y hora: " + date + "\n"
                        + "Lugar: " + location + ", " + city + "\n"
                        + "Participantes: " + participantCount + "\n"
                        + "Total: " + price + "\n\n"
                        + "Te esperamos. Podés consultar esta reserva desde la sección Mis reservas.",
                "casa-pueblo.png");
    }
}
