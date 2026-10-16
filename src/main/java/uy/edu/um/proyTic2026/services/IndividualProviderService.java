package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.IndividualProviderRegistrationRequest;
import uy.edu.um.proyTic2026.entities.IndividualProvider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.repositories.IndividualProviderRepository;
import uy.edu.um.proyTic2026.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class IndividualProviderService {
    private final UserRepository userRepository;
    private final IndividualProviderRepository individualProviderRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerIndividual(IndividualProviderRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (individualProviderRepository.existsByCi(request.getCi())) {
            throw new IllegalArgumentException("La cédula ya está registrada");
        }

        IndividualProvider individual = new IndividualProvider();
        individual.setUsername(request.getUsername());
        individual.setEmail(request.getEmail());
        individual.setPassword(passwordEncoder.encode(request.getPassword()));
        individual.setPhone(request.getPhone());
        individual.setDepartment(request.getDepartment());
        individual.setCity(request.getCity());
        individual.setAddress(request.getAddress());
        individual.setFirstName(request.getFirstName());
        individual.setLastName(request.getLastName());
        individual.setCi(request.getCi());
        individual.setStatus(ProviderStatus.PENDING);
        individualProviderRepository.save(individual);
    }
}
