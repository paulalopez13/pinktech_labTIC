package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.*;
import uy.edu.um.proyTic2026.entities.ActivityRecurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.math.BigDecimal;

public record ActivityRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String department,
        @NotBlank @Size(max = 50) String city,
        @NotBlank @Size(max = 180) String location,
        @NotBlank @Size(max = 3000) String description,
        @NotEmpty List<@NotNull Long> categoryIds,
        @NotNull @FutureOrPresent LocalDate firstDate,
        @NotNull ActivityRecurrence recurrence,
        LocalDate repeatUntil,
        @NotEmpty List<@NotNull LocalTime> startTimes,
        List<DayOfWeek> weekdays,
        @NotNull @Min(1) @Max(10000) Integer capacity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull @Min(1) @Max(1440) Integer durationMinutes,
        @NotNull @Min(0) @Max(720) Integer bookingCutoffHours,
        @Size(max = 500) String equipment,
        @Min(0) @Max(120) Integer minimumAge,
        @Min(0) @Max(120) Integer maximumAge,
        @Min(0) @Max(300) Integer minimumHeightCm,
        @Min(0) @Max(300) Integer maximumHeightCm,
        @Size(max = 30) String difficulty,
        @Size(max = 1000) String otherRequirements
) { }
