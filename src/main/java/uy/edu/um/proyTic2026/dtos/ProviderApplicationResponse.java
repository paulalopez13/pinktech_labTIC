package uy.edu.um.proyTic2026.dtos;

import uy.edu.um.proyTic2026.entities.Provider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;

public record ProviderApplicationResponse(
        Long id,
        String providerType,
        String username,
        String email,
        String firstName,
        String lastName,
        String ci,
        String companyName,
        String rut,
        String phone,
        String department,
        String city,
        String address,
        ProviderStatus status
) {
    public static ProviderApplicationResponse from(Provider provider) {
        String type = provider instanceof uy.edu.um.proyTic2026.entities.CompanyProvider ? "EMPRESA" : "PARTICULAR";
        var individual = provider instanceof uy.edu.um.proyTic2026.entities.IndividualProvider person ? person : null;
        var company = provider instanceof uy.edu.um.proyTic2026.entities.CompanyProvider business ? business : null;
        return new ProviderApplicationResponse(provider.getId(), type, provider.getUsername(), provider.getEmail(),
                individual == null ? null : individual.getFirstName(),
                individual == null ? null : individual.getLastName(),
                individual == null ? null : individual.getCi(),
                company == null ? null : company.getCompanyName(),
                company == null ? null : company.getRut(), provider.getPhone(), provider.getDepartment(),
                provider.getCity(), provider.getAddress(), provider.getStatus());
    }
}
