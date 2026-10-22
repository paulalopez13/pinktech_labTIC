package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tourists")
@Getter
@Setter
@NoArgsConstructor
public class Tourist extends User {

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(length = 12)
    private DocumentType documentType = DocumentType.CI;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String lastName;

    @NotBlank
    @Size(max = 9)
    @Column(nullable = false, unique = true, length = 20)
    private String ci;

    @Min(0)
    @Max(120)
    @Column(nullable = false)
    private Integer age;
}
