package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.edu.um.proyTic2026.entities.DocumentType;

public record ReservationParticipantRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotNull DocumentType documentType,
        @NotBlank @Size(min = 1, max = 9) @Pattern(regexp = "[A-Za-z0-9]+") String documentNumber,
        @NotNull @Min(0) @Max(120) Integer age,
        @Min(30) @Max(250) Integer heightCm,
        @Pattern(regexp = "|Principiante|Intermedio|Avanzado") String level
) { }
