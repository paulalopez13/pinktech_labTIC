package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uy.edu.um.proyTic2026.entities.DocumentType;

@Getter
@Setter
@NoArgsConstructor
public class ProfileUpdateRequest {
    @Size(min = 3, max = 30)
    private String username;
    @Email @Size(max = 100)
    private String email;
    private String password;
    private String confirmPassword;
    @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lastName;
    @Size(max = 9)
    private String ci;
    private DocumentType documentType;
    @Min(0) @Max(120)
    private Integer age;
    @Size(max = 20) @Pattern(regexp = "\\+[1-9]\\d{5,14}")
    private String phone;
    @Size(max = 30)
    private String department;
    @Size(max = 30)
    private String city;
    @Size(max = 150)
    private String address;
    @Size(max = 30)
    private String companyName;
    @Size(max = 20)
    @Pattern(regexp = "\\d{12}")
    private String rut;
    @Size(max = 250)
    private String website;
    @Size(max = 100)
    private String instagram;
    @Size(max = 100)
    private String twitter;
    @Size(max = 30)
    private String whatsapp;
    @Size(max = 100)
    private String youtube;
    @Size(max = 100)
    private String facebook;
}
