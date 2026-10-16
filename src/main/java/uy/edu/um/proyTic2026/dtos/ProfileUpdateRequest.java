package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Size(max = 20)
    private String ci;
    @Min(0) @Max(120)
    private Integer age;
    @Size(max = 20)
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
    private String rut;
}
