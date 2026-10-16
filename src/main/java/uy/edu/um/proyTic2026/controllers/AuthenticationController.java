package uy.edu.um.proyTic2026.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import uy.edu.um.proyTic2026.dtos.AuthenticatedUserResponse;
import uy.edu.um.proyTic2026.dtos.LoginRequest;
import uy.edu.um.proyTic2026.dtos.ForgotPasswordRequest;
import uy.edu.um.proyTic2026.dtos.ResetPasswordRequest;
import uy.edu.um.proyTic2026.services.AuthenticationService;
import uy.edu.um.proyTic2026.services.LoginException;
import uy.edu.um.proyTic2026.services.PasswordResetService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            return ResponseEntity.ok(Map.of("message", passwordResetService.requestReset(request.email())));
        } catch (IllegalStateException error) {
            return ResponseEntity.status(503).body(Map.of("message", error.getMessage()));
        } catch (org.springframework.mail.MailException error) {
            return ResponseEntity.status(503).body(Map.of("message", "No se pudo enviar el correo. Probá más tarde."));
        }
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request);
            return ResponseEntity.ok(Map.of("message", "La contraseña se cambió. Iniciá sesión con la nueva contraseña."));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        try {
            AuthenticatedUserResponse user = authenticationService.login(request);
            session.setAttribute("userId", user.id());
            return ResponseEntity.ok(user);
        } catch (LoginException error) {
            return ResponseEntity.status(error.getStatus())
                    .body(Map.of("message", error.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof Long id)) {
            return ResponseEntity.status(401).body(Map.of("message", "No hay una sesión iniciada"));
        }
        try {
            return ResponseEntity.ok(authenticationService.getUser(id));
        } catch (LoginException error) {
            session.invalidate();
            return ResponseEntity.status(error.getStatus()).body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken(), "headerName", csrfToken.getHeaderName());
    }
}
