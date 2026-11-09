package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationSeatHoldRequest(
        @NotNull LocalDateTime activityStart,
        @NotNull @Min(1) Integer participantCount
) { }
