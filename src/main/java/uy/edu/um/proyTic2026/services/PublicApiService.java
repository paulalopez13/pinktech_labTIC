package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ProviderPublicResponse;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.repositories.ProviderRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicApiService {
    private final ProviderRepository providerRepository;
    private final ActivityService activityService;

    // 1. Listado de todos los emprendimientos aprobados.
    @Transactional(readOnly = true)
    public List<ProviderPublicResponse> providers() {
        return providerRepository.findAllByStatusOrderByIdAsc(ProviderStatus.APPROVED).stream()
                .map(provider -> activityService.publicProvider(provider.getId())).toList();
    }
}
