package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import jakarta.validation.constraints.Email;


@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    @NotBlank
    @Size(min = 3, max = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank
    @Email
    private String email;

    @Column(nullable = false, length = 100)
    @NotBlank
    // Aquí se guarda el hash BCrypt; la longitud original se valida en el DTO.
    @Size(max = 100)
    private String password;



}
