package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uy.edu.um.proyTic2026.entities.DocumentType;

@Getter
@Setter
@NoArgsConstructor
public class TouristRegistrationRequest {
    @NotBlank @Size(min = 3, max = 30)
    private String username;
    @NotBlank @Email @Size(max = 100)
    private String email;
    @NotBlank @Size(min = 8, max = 30)
    private String password;
    @NotBlank @Size(min = 8, max = 30)
    private String confirmPassword;
    @NotNull
    private DocumentType documentType;
    @NotBlank @Size(max = 50)
    private String firstName;
    @NotBlank @Size(max = 50)
    private String lastName;
    @NotBlank @Size(max = 9)
    private String ci;
    @NotNull @Min(0) @Max(120)
    private Integer age;
}
