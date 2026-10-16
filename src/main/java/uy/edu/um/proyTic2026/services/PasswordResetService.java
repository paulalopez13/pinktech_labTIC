package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import uy.edu.um.proyTic2026.dtos.ResetPasswordRequest;
import uy.edu.um.proyTic2026.entities.PasswordResetToken;
import uy.edu.um.proyTic2026.repositories.PasswordResetTokenRepository;
import uy.edu.um.proyTic2026.repositories.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final long TOKEN_LIFETIME_SECONDS = 20 * 60;
    private static final long REQUEST_COOLDOWN_SECONDS = 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String GENERIC_MESSAGE = "Si el email está registrado, recibirás un enlace para recuperar la contraseña.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordResetEmailService resetEmailService;
    private final ConcurrentHashMap<String, Instant> lastRequestByEmailHash = new ConcurrentHashMap<>();

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.password-reset.frontend-url:http://127.0.0.1:5173}")
    private String frontendUrl;

    public String requestReset(String email) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || smtpHost == null || smtpHost.isBlank()
                || fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("El correo de recuperación no está configurado en el servidor.");
        }

        Instant now = Instant.now();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String throttleKey = sha256(normalizedEmail);
        AtomicBoolean limited = new AtomicBoolean();
        lastRequestByEmailHash.compute(throttleKey, (key, lastRequest) -> {
            if (lastRequest != null && lastRequest.plusSeconds(REQUEST_COOLDOWN_SECONDS).isAfter(now)) {
                limited.set(true);
                return lastRequest;
            }
            return now;
        });
        if (limited.get()) return GENERIC_MESSAGE;

        var user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) return GENERIC_MESSAGE;

        tokenRepository.deleteAllByExpiresAtBefore(now);
        tokenRepository.deleteAllByUser_Id(user.getId());

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(sha256(rawToken));
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plusSeconds(TOKEN_LIFETIME_SECONDS));
        tokenRepository.save(resetToken);

        String resetLink = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("token", rawToken).build().encode().toUriString();
        resetEmailService.send(user.getEmail(), fromAddress, resetLink);
        return GENERIC_MESSAGE;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        String tokenHash = sha256(request.token());
        PasswordResetToken resetToken = tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(this::invalidToken);
        if (!resetToken.getExpiresAt().isAfter(Instant.now())) {
            tokenRepository.delete(resetToken);
            throw invalidToken();
        }
        var user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        tokenRepository.deleteAllByUser_Id(user.getId());
        lastRequestByEmailHash.remove(sha256(user.getEmail().toLowerCase(Locale.ROOT)));
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException("El enlace venció o ya fue usado. Pedí uno nuevo.");
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo preparar la recuperación de contraseña.", error);
        }
    }
}
