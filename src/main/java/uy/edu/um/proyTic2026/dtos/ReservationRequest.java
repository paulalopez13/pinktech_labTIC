package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReservationRequest(@NotNull LocalDateTime activityStart) { }
