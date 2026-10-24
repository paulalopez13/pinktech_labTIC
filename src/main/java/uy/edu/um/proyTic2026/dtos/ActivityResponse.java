package uy.edu.um.proyTic2026.dtos;

import uy.edu.um.proyTic2026.entities.Activity;
import uy.edu.um.proyTic2026.entities.ActivityRecurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.math.BigDecimal;

public record ActivityResponse(
        Long id, String name, String providerName, String department, String city, String location,
        String description, List<ActivityCategoryResponse> categories, LocalDate firstDate,
        ActivityRecurrence recurrence, LocalDate repeatUntil, List<LocalTime> startTimes,
        List<DayOfWeek> weekdays, List<LocalDateTime> upcomingOccurrences, int capacity, BigDecimal price,
        int durationMinutes, int bookingCutoffMinutes, String equipment, Integer minimumAge,
        Integer maximumAge, Integer minimumHeightCm, Integer maximumHeightCm, String difficulty,
        String otherRequirements, double averageRating, int reviewCount, long recentReservationCount,
        Long providerId, List<ActivityPhotoResponse> photos
) {
    public static ActivityResponse from(Activity activity, List<LocalDateTime> occurrences,
                                        double averageRating, int reviewCount, long recentReservationCount) {
        return new ActivityResponse(activity.getId(), activity.getName(), providerName(activity.getProvider()),
                activity.getDepartment(), activity.getCity(), activity.getLocation(), activity.getDescription(),
                activity.getCategories().stream().map(ActivityCategoryResponse::from).sorted(
                        java.util.Comparator.comparing(ActivityCategoryResponse::name, String.CASE_INSENSITIVE_ORDER)).toList(),
                activity.getFirstDate(), activity.getRecurrence(), activity.getRepeatUntil(),
                List.copyOf(activity.getStartTimes()), activity.getWeekdays().stream().sorted().toList(), occurrences,
                activity.getCapacity(), activity.getPrice(), activity.getDurationMinutes(), activity.getBookingCutoffMinutes(),
                activity.getEquipment(), activity.getMinimumAge(), activity.getMaximumAge(),
                activity.getMinimumHeightCm(), activity.getMaximumHeightCm(), activity.getDifficulty(),
                activity.getOtherRequirements(), averageRating, reviewCount, recentReservationCount,
                activity.getProvider().getId(),
                activity.getPhotos().stream().map(photo -> ActivityPhotoResponse.from(activity.getId(), photo)).toList());
    }

    private static String providerName(uy.edu.um.proyTic2026.entities.Provider provider) {
        if (provider instanceof uy.edu.um.proyTic2026.entities.IndividualProvider individual)
            return individual.getFirstName() + " " + individual.getLastName();
        if (provider instanceof uy.edu.um.proyTic2026.entities.CompanyProvider company)
            return company.getCompanyName();
        return provider.getUsername();
    }
}
