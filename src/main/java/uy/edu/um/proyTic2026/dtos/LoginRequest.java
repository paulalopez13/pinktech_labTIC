package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {
    @NotBlank
    @Size(max = 100)
    private String identifier;

    @NotBlank
    @Size(max = 100)
    private String password;
}
