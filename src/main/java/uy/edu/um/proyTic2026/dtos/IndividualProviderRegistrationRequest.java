package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import uy.edu.um.proyTic2026.entities.DocumentType;

@Getter
@Setter
@NoArgsConstructor
public class IndividualProviderRegistrationRequest {
    @NotBlank @Size(min = 3, max = 30)
    private String username;
    @NotBlank @Email @Size(max = 100)
    private String email;
    @NotBlank @Size(min = 8, max = 30)
    private String password;
    @NotBlank @Size(min = 8, max = 30)
    private String confirmPassword;
    @NotBlank @Size(max = 20) @Pattern(regexp = "\\+[1-9]\\d{5,14}")
    private String phone;
    @NotBlank @Size(max = 30)
    private String department;
    @NotBlank @Size(max = 30)
    private String city;
    @NotBlank @Size(max = 150)
    private String address;
    @NotBlank @Size(max = 20)
    private String firstName;
    @NotBlank @Size(max = 20)
    private String lastName;
    @NotBlank @Size(max = 9)
    private String ci;
    @NotNull
    private DocumentType documentType;
    @Size(max = 250) private String website;
    @Size(max = 100) private String instagram;
    @Size(max = 100) private String twitter;
    @Size(max = 30) private String whatsapp;
    @Size(max = 100) private String youtube;
    @Size(max = 100) private String facebook;
}
