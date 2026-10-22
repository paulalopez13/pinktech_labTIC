package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record GroupReservationRequest(
        @NotNull LocalDateTime activityStart,
        @NotBlank @Size(max = 100) String contactName,
        @NotBlank @Email @Size(max = 100) String contactEmail,
        @NotBlank @Pattern(regexp = "[+0-9 ()-]{7,30}") String contactPhone,
        @NotEmpty @Size(max = 100) List<@Valid ReservationParticipantRequest> participants
) { }
