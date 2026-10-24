package uy.edu.um.proyTic2026;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uy.edu.um.proyTic2026.dtos.ProviderPublicResponse;
import uy.edu.um.proyTic2026.entities.Provider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.repositories.ProviderRepository;
import uy.edu.um.proyTic2026.services.PublicApiService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Prueba "listado de emprendimientos" (API pública) con los prestadores de los datos de demostración.
@SpringBootTest
class PublicApiProvidersTests {

    @Autowired PublicApiService publicApiService;
    @Autowired ProviderRepository providerRepository;

    @Test
    void listsEveryApprovedProvider() {
        List<Long> approvedIds = providerRepository.findAllByStatusOrderByIdAsc(ProviderStatus.APPROVED)
                .stream().map(Provider::getId).toList();

        List<Long> listedIds = publicApiService.providers().stream().map(ProviderPublicResponse::id).toList();

        assertFalse(listedIds.isEmpty());
        assertEquals(approvedIds, listedIds);
    }

    @Test
    void leavesOutPendingAndRejectedProviders() {
        List<Long> listedIds = publicApiService.providers().stream().map(ProviderPublicResponse::id).toList();

        for (ProviderStatus status : List.of(ProviderStatus.PENDING, ProviderStatus.REJECTED)) {
            List<Provider> hidden = providerRepository.findAllByStatusOrderByIdAsc(status);
            assertFalse(hidden.isEmpty(), "Los datos de demostración deberían tener prestadores " + status);
            hidden.forEach(provider -> assertFalse(listedIds.contains(provider.getId())));
        }
    }

    @Test
    void includesTheDisplayName() {
        assertTrue(publicApiService.providers().stream()
                .anyMatch(provider -> provider.displayName().equals("Prestador Demo")));
    }
}
