package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(min = 8, max = 30) String password,
        @NotBlank @Size(min = 8, max = 30) String confirmPassword
) { }
