package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompanyProviderRegistrationRequest {

    @NotBlank
    @Size(min = 3, max = 30)
    private String username;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 8, max = 30)
    private String password;

    @NotBlank
    @Size(min = 8, max = 30)
    private String confirmPassword;

    @NotBlank
    @Size(max = 20)
    private String phone;

    @NotBlank
    @Size(max = 30)
    private String department;

    @NotBlank
    @Size(max = 30)
    private String city;

    @NotBlank
    @Size(max = 150)
    private String address;

    @NotBlank
    @Size(max = 30)
    private String companyName;

    @NotBlank
    @Size(max = 20)
    private String rut;
}
