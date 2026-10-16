package uy.edu.um.proyTic2026.dtos;

public record UserProfileResponse(
        String username,
        String email,
        String role,
        String providerStatus,
        String firstName,
        String lastName,
        String ci,
        Integer age,
        String phone,
        String department,
        String city,
        String address,
        String companyName,
        String rut) {
}
