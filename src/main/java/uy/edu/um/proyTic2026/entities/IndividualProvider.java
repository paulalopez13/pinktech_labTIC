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
@Table(name = "individual_providers")
@Getter
@Setter
@NoArgsConstructor

public class IndividualProvider extends Provider {

    @NotBlank
    @Size(max = 20)
    @Column(length = 20)
    private String firstName;

    @NotBlank
    @Size(max = 20)
    @Column(length = 20)
    private String lastName;

    @NotBlank
    @Size(max = 20)
    @Column(unique = true, length = 20)
    private String ci;

}
