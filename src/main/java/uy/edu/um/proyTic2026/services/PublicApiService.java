package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ProviderPublicResponse;
import uy.edu.um.proyTic2026.dtos.ProviderVerificationResponse;
import uy.edu.um.proyTic2026.entities.CompanyProvider;
import uy.edu.um.proyTic2026.entities.Provider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.repositories.CompanyProviderRepository;
import uy.edu.um.proyTic2026.repositories.IndividualProviderRepository;
import uy.edu.um.proyTic2026.repositories.ProviderRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PublicApiService {
    private final ProviderRepository providerRepository;
    private final CompanyProviderRepository companyProviderRepository;
    private final IndividualProviderRepository individualProviderRepository;
    private final ActivityService activityService;

    // 1. Listado de todos los emprendimientos aprobados.
    @Transactional(readOnly = true)
    public List<ProviderPublicResponse> providers() {
        return providerRepository.findAllByStatusOrderByIdAsc(ProviderStatus.APPROVED).stream()
                .map(provider -> activityService.publicProvider(provider.getId())).toList();
    }

    // 4. Verificar un emprendimiento por su RUT (empresa) o cédula (prestador particular).
    @Transactional(readOnly = true)
    public ProviderVerificationResponse verify(String document) {
        if (document == null || document.isBlank()) {
            throw new IllegalArgumentException("Indicá el RUT o la cédula del emprendimiento.");
        }
        String clean = document.trim();

        // Primero se busca como empresa (RUT); si no aparece, como particular (cédula).
        Optional<Provider> found = companyProviderRepository.findByRut(clean).map(Provider.class::cast)
                .or(() -> individualProviderRepository.findByCi(clean));

        if (found.isEmpty()) {
            return new ProviderVerificationResponse(clean, false, false, null, null, null);
        }
        Provider provider = found.get();
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            return new ProviderVerificationResponse(clean, true, false, null, null, null);
        }
        // Se reutiliza el perfil público para obtener el nombre que se muestra.
        ProviderPublicResponse profile = activityService.publicProvider(provider.getId());
        return new ProviderVerificationResponse(clean, true, true, provider.getId(), profile.displayName(),
                provider instanceof CompanyProvider ? "Empresa" : "Prestador independiente");
    }
}
