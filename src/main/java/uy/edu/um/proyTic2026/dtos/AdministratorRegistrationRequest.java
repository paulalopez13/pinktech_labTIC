package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdministratorRegistrationRequest(
        @NotBlank @Size(min = 3, max = 30) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 30) String password,
        @NotBlank @Size(min = 8, max = 30) String confirmPassword
) {}
