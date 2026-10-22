package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }
        Administrator administrator = new Administrator();
        administrator.setUsername(request.username().trim());
        administrator.setEmail(request.email().trim());
        administrator.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(administrator);
        accountEmailService.administratorCreated(administrator.getEmail(), administrator.getUsername());
    }
}
