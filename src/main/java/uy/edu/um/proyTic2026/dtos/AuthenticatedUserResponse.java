package uy.edu.um.proyTic2026.dtos;

public record AuthenticatedUserResponse(Long id, String username, String role, String providerStatus) {
}
