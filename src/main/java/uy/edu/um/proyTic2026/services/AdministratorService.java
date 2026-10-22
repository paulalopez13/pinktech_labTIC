package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uy.edu.um.proyTic2026.dtos.AdministratorRegistrationRequest;
import uy.edu.um.proyTic2026.entities.Administrator;
import uy.edu.um.proyTic2026.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AdministratorService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService accountEmailService;

    @Transactional
    public void create(AdministratorRegistrationRequest request) {
        PasswordPolicy.requireStrong(request.password());
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario o el email ya está registrado.");
        }
        Administrator administrator = new Administrator();
        administrator.setUsername(username);
        administrator.setEmail(email);
        administrator.setPassword(passwordEncoder.encode(request.password()));
        Administrator saved = userRepository.save(administrator);
        String assignedPassword = request.password();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                accountEmailService.administratorCreated(saved.getEmail(), saved.getUsername(), assignedPassword);
            }
        });
    }
}
