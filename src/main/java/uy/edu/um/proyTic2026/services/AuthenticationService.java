package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uy.edu.um.proyTic2026.dtos.AuthenticatedUserResponse;
import uy.edu.um.proyTic2026.dtos.LoginRequest;
import uy.edu.um.proyTic2026.entities.Administrator;
import uy.edu.um.proyTic2026.entities.Provider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.entities.Tourist;
import uy.edu.um.proyTic2026.entities.User;
import uy.edu.um.proyTic2026.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticatedUserResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                request.getIdentifier(), request.getIdentifier()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new LoginException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }

        return toResponse(user);
    }

    public AuthenticatedUserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new LoginException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida"));
        return toResponse(user);
    }

    private AuthenticatedUserResponse toResponse(User user) {
        String role;
        if (user instanceof Tourist) {
            role = "TOURIST";
        } else if (user instanceof Provider) {
            role = "PROVIDER";
        } else if (user instanceof Administrator) {
            role = "ADMINISTRATOR";
        } else {
            throw new LoginException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }

        if (user instanceof Provider provider && provider.getStatus() != ProviderStatus.APPROVED) {
            String message = provider.getStatus() == ProviderStatus.PENDING
                    ? "Tu cuenta de prestador todavía está pendiente de aprobación."
                    : "Tu cuenta de prestador fue rechazada y no puede iniciar sesión.";
            throw new LoginException(HttpStatus.FORBIDDEN, message);
        }

        String providerStatus = user instanceof Provider provider
                ? provider.getStatus().name()
                : null;
        return new AuthenticatedUserResponse(user.getId(), user.getUsername(), role, providerStatus);
    }
}
