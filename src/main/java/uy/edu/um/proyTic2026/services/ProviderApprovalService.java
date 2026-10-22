package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ProviderApplicationResponse;
import uy.edu.um.proyTic2026.entities.CompanyProvider;
import uy.edu.um.proyTic2026.entities.IndividualProvider;
import uy.edu.um.proyTic2026.entities.Provider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.repositories.CompanyProviderRepository;
import uy.edu.um.proyTic2026.repositories.IndividualProviderRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderApprovalService {
    private final CompanyProviderRepository companyRepository;
    private final IndividualProviderRepository individualRepository;
    private final AccountEmailService accountEmailService;

    @Transactional(readOnly = true)
    public List<ProviderApplicationResponse> listByStatus(ProviderStatus status) {
        var companies = companyRepository.findAllByStatusOrderByIdAsc(status);
        var individuals = individualRepository.findAllByStatusOrderByIdAsc(status);
        return java.util.stream.Stream.concat(companies.stream(), individuals.stream())
                .sorted(Comparator.comparing(Provider::getId))
                .map(ProviderApplicationResponse::from)
                .toList();
    }

    @Transactional
    public ProviderApplicationResponse decide(Long providerId, ProviderStatus decision) {
        Provider provider = companyRepository.findById(providerId).map(p -> (Provider) p)
                .or(() -> individualRepository.findById(providerId).map(p -> (Provider) p))
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la solicitud del prestador"));
        if (provider.getStatus() != ProviderStatus.PENDING) {
            throw new IllegalArgumentException("Esta solicitud ya fue revisada");
        }
        provider.setStatus(decision);
        if (decision == ProviderStatus.APPROVED) {
            accountEmailService.providerApproved(provider.getEmail(), provider.getUsername());
        }
        return ProviderApplicationResponse.from(provider);
    }
}
