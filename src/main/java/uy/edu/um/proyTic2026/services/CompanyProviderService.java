package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uy.edu.um.proyTic2026.repositories.CompanyProviderRepository;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.CompanyProviderRegistrationRequest;
import uy.edu.um.proyTic2026.entities.CompanyProvider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;

@Service
@RequiredArgsConstructor
public class CompanyProviderService {

    private final UserRepository userRepository;

    private final CompanyProviderRepository companyProviderRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerCompany( CompanyProviderRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        // Comprobamos que el username no esté registrado.
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }

        // Comprobamos que el email no esté registrado.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // Comprobamos que el RUT no pertenezca a otra empresa.
        if (companyProviderRepository.existsByRut(request.getRut())) {
            throw new IllegalArgumentException("El RUT ya está registrado");
        }

        // Creamos la entidad que se guardará en la base de datos.
        CompanyProvider company = new CompanyProvider();

        // Asignamos los datos
        company.setUsername(request.getUsername());
        company.setEmail(request.getEmail());
        company.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        company.setPhone(request.getPhone());
        company.setDepartment(request.getDepartment());
        company.setCity(request.getCity());
        company.setAddress(request.getAddress());
        company.setStatus(ProviderStatus.PENDING);
        company.setCompanyName(request.getCompanyName());
        company.setRut(request.getRut());

        companyProviderRepository.save(company);

    }
}
