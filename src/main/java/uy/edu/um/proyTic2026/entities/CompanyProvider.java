package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "company_providers")
@Getter
@Setter
@NoArgsConstructor

public class CompanyProvider extends Provider {

    @NotBlank
    @Size(max = 30)
    @Column(length = 30)
    private String companyName;

    @NotBlank
    @Size(max = 20)
    @Column(unique = true, length = 20)
    private String rut;
}
