package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ProfileUpdateRequest;
import uy.edu.um.proyTic2026.dtos.UserProfileResponse;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.*;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final TouristRepository touristRepository;
    private final IndividualProviderRepository individualProviderRepository;
    private final CompanyProviderRepository companyProviderRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return toResponse(getUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = getUser(userId);
        String username = required(request.getUsername(), "El nombre de usuario es obligatorio");
        String email = required(request.getEmail(), "El email es obligatorio");

        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, userId)) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        user.setUsername(username);
        user.setEmail(email);

        boolean passwordProvided = request.getPassword() != null && !request.getPassword().isBlank();
        boolean confirmationProvided = request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank();
        if (passwordProvided) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("Las contraseñas no coinciden");
            }
            PasswordPolicy.requireStrong(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else if (confirmationProvided) {
            throw new IllegalArgumentException("Ingresá una contraseña nueva para confirmarla");
        }

        if (user instanceof Tourist tourist) {
            updateTourist(tourist, request, userId);
        } else if (user instanceof IndividualProvider provider) {
            updateIndividualProvider(provider, request, userId);
        } else if (user instanceof CompanyProvider provider) {
            updateCompanyProvider(provider, request, userId);
        }

        userRepository.save(user);
        return toResponse(user);
    }

    private void updateTourist(Tourist tourist, ProfileUpdateRequest request, Long userId) {
        String ci = required(request.getCi(), "La cédula es obligatoria");
        DocumentType documentType = resolveDocumentType(request.getDocumentType(), tourist.getDocumentType());
        validateDocument(documentType, ci);
        if (touristRepository.existsByCiAndIdNot(ci, userId)
                || individualProviderRepository.existsByCiAndIdNot(ci, userId)) {
            throw new IllegalArgumentException("La cédula ya está registrada");
        }
        tourist.setFirstName(required(request.getFirstName(), "El nombre es obligatorio"));
        tourist.setLastName(required(request.getLastName(), "El apellido es obligatorio"));
        tourist.setCi(ci);
        tourist.setDocumentType(documentType);
        if (request.getAge() == null || request.getAge() < 0 || request.getAge() > 120) {
            throw new IllegalArgumentException("La edad debe estar entre 0 y 120 años");
        }
        tourist.setAge(request.getAge());
    }

    private void updateIndividualProvider(IndividualProvider provider, ProfileUpdateRequest request, Long userId) {
        String ci = required(request.getCi(), "La cédula es obligatoria");
        DocumentType documentType = resolveDocumentType(request.getDocumentType(), provider.getDocumentType());
        validateDocument(documentType, ci);
        if (individualProviderRepository.existsByCiAndIdNot(ci, userId)
                || touristRepository.existsByCiAndIdNot(ci, userId)) {
            throw new IllegalArgumentException("La cédula ya está registrada");
        }
        provider.setFirstName(required(request.getFirstName(), "El nombre es obligatorio"));
        provider.setLastName(required(request.getLastName(), "El apellido es obligatorio"));
        provider.setCi(ci);
        provider.setDocumentType(documentType);
        updateProviderAddress(provider, request);
    }

    private void updateCompanyProvider(CompanyProvider provider, ProfileUpdateRequest request, Long userId) {
        String rut = required(request.getRut(), "El RUT es obligatorio");
        if (companyProviderRepository.existsByRutAndIdNot(rut, userId)) {
            throw new IllegalArgumentException("El RUT ya está registrado");
        }
        provider.setCompanyName(required(request.getCompanyName(), "El nombre de la empresa es obligatorio"));
        provider.setRut(rut);
        updateProviderAddress(provider, request);
    }

    private void updateProviderAddress(Provider provider, ProfileUpdateRequest request) {
        provider.setPhone(required(request.getPhone(), "El teléfono es obligatorio"));
        provider.setDepartment(required(request.getDepartment(), "El departamento es obligatorio"));
        provider.setCity(required(request.getCity(), "La ciudad es obligatoria"));
        provider.setAddress(required(request.getAddress(), "La dirección es obligatoria"));
        provider.setWebsite(optional(request.getWebsite()));
        provider.setInstagram(optional(request.getInstagram()));
        provider.setTwitter(optional(request.getTwitter()));
        provider.setWhatsapp(optional(request.getWhatsapp()));
        provider.setYoutube(optional(request.getYoutube()));
        provider.setFacebook(optional(request.getFacebook()));
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private DocumentType resolveDocumentType(DocumentType requested, DocumentType current) {
        return requested != null ? requested : current != null ? current : DocumentType.CI;
    }

    private void validateDocument(DocumentType type, String number) {
        String pattern = type == DocumentType.CI ? "\\d{1,8}" : "[A-Za-z0-9]{1,9}";
        if (!number.matches(pattern)) {
            throw new IllegalArgumentException("El documento no tiene un formato válido");
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new LoginException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "La sesión ya no es válida"));
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private UserProfileResponse toResponse(User user) {
        String role = user instanceof Tourist ? "TOURIST"
                : user instanceof Provider ? "PROVIDER"
                : user instanceof Administrator ? "ADMINISTRATOR" : "USER";
        String providerStatus = user instanceof Provider provider ? provider.getStatus().name() : null;
        String firstName = null, lastName = null, ci = null, phone = null, documentType = null;
        Integer age = null;
        String department = null, city = null, address = null, companyName = null, rut = null;
        String website = null, instagram = null, twitter = null, whatsapp = null, youtube = null, facebook = null;

        if (user instanceof Tourist tourist) {
            firstName = tourist.getFirstName(); lastName = tourist.getLastName();
            ci = tourist.getCi(); age = tourist.getAge();
            documentType = resolveDocumentType(null, tourist.getDocumentType()).name();
        } else if (user instanceof IndividualProvider provider) {
            firstName = provider.getFirstName(); lastName = provider.getLastName(); ci = provider.getCi();
            documentType = resolveDocumentType(null, provider.getDocumentType()).name();
            phone = provider.getPhone(); department = provider.getDepartment(); city = provider.getCity();
            address = provider.getAddress();
            website = provider.getWebsite(); instagram = provider.getInstagram(); twitter = provider.getTwitter();
            whatsapp = provider.getWhatsapp(); youtube = provider.getYoutube(); facebook = provider.getFacebook();
        } else if (user instanceof CompanyProvider provider) {
            companyName = provider.getCompanyName(); rut = provider.getRut();
            phone = provider.getPhone(); department = provider.getDepartment(); city = provider.getCity();
            address = provider.getAddress();
            website = provider.getWebsite(); instagram = provider.getInstagram(); twitter = provider.getTwitter();
            whatsapp = provider.getWhatsapp(); youtube = provider.getYoutube(); facebook = provider.getFacebook();
        }

        return new UserProfileResponse(user.getUsername(), user.getEmail(), role, providerStatus,
                firstName, lastName, ci, documentType, age, phone, department, city, address, companyName, rut,
                website, instagram, twitter, whatsapp, youtube, facebook);
    }
}
