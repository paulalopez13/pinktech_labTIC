package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.TouristRegistrationRequest;
import uy.edu.um.proyTic2026.entities.Tourist;
import uy.edu.um.proyTic2026.repositories.TouristRepository;
import uy.edu.um.proyTic2026.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class TouristService {
    private final UserRepository userRepository;
    private final TouristRepository touristRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService accountEmailService;

    @Transactional
    public void registerTourist(TouristRegistrationRequest request) {
        PasswordPolicy.requireStrong(request.getPassword());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (touristRepository.existsByCi(request.getCi())) {
            throw new IllegalArgumentException("La cédula ya está registrada");
        }
        validateDocument(request.getDocumentType(), request.getCi());

        Tourist tourist = new Tourist();
        tourist.setUsername(request.getUsername());
        tourist.setEmail(request.getEmail());
        tourist.setPassword(passwordEncoder.encode(request.getPassword()));
        tourist.setFirstName(request.getFirstName());
        tourist.setLastName(request.getLastName());
        tourist.setCi(request.getCi());
        tourist.setDocumentType(request.getDocumentType());
        tourist.setAge(request.getAge());
        touristRepository.save(tourist);
        accountEmailService.touristRegistered(tourist.getEmail(), tourist.getUsername());
    }

    private void validateDocument(uy.edu.um.proyTic2026.entities.DocumentType type, String number) {
        String pattern = type == uy.edu.um.proyTic2026.entities.DocumentType.CI ? "\\d{1,8}" : "[A-Za-z0-9]{1,9}";
        if (type == null || number == null || !number.matches(pattern)) {
            throw new IllegalArgumentException("El documento no tiene un formato válido");
        }
    }
}
