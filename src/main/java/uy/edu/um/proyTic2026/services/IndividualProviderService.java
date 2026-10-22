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
    private final AccountEmailService accountEmailService;

    @Transactional
    public void registerIndividual(IndividualProviderRegistrationRequest request) {
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
        if (individualProviderRepository.existsByCi(request.getCi())) {
            throw new IllegalArgumentException("La cédula ya está registrada");
        }
        validateDocument(request.getDocumentType(), request.getCi());

        IndividualProvider individual = new IndividualProvider();
        individual.setUsername(request.getUsername());
        individual.setEmail(request.getEmail());
        individual.setPassword(passwordEncoder.encode(request.getPassword()));
        individual.setPhone(request.getPhone());
        individual.setDepartment(request.getDepartment());
        individual.setCity(request.getCity());
        individual.setAddress(request.getAddress());
        individual.setWebsite(optional(request.getWebsite()));
        individual.setInstagram(optional(request.getInstagram()));
        individual.setTwitter(optional(request.getTwitter()));
        individual.setWhatsapp(optional(request.getWhatsapp()));
        individual.setYoutube(optional(request.getYoutube()));
        individual.setFacebook(optional(request.getFacebook()));
        individual.setFirstName(request.getFirstName());
        individual.setLastName(request.getLastName());
        individual.setCi(request.getCi());
        individual.setDocumentType(request.getDocumentType());
        individual.setStatus(ProviderStatus.PENDING);
        individualProviderRepository.save(individual);
        accountEmailService.providerRequested(individual.getEmail(), individual.getUsername());
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateDocument(uy.edu.um.proyTic2026.entities.DocumentType type, String number) {
        String pattern = type == uy.edu.um.proyTic2026.entities.DocumentType.CI ? "\\d{1,8}" : "[A-Za-z0-9]{1,9}";
        if (type == null || number == null || !number.matches(pattern)) {
            throw new IllegalArgumentException("El documento no tiene un formato válido");
        }
    }
}
