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

    @Transactional
    public void registerTourist(TouristRegistrationRequest request) {
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

        Tourist tourist = new Tourist();
        tourist.setUsername(request.getUsername());
        tourist.setEmail(request.getEmail());
        tourist.setPassword(passwordEncoder.encode(request.getPassword()));
        tourist.setFirstName(request.getFirstName());
        tourist.setLastName(request.getLastName());
        tourist.setCi(request.getCi());
        tourist.setAge(request.getAge());
        touristRepository.save(tourist);
    }
}
