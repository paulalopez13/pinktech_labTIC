package uy.edu.um.proyTic2026.dtos;

import uy.edu.um.proyTic2026.entities.ActivityReservation;
import java.time.LocalDateTime;

public record ReservationResponse(Long id, Long activityId, String activityName, String department,
                                  String city, String location, LocalDateTime activityStart,
                                  int durationMinutes, boolean reviewAllowed, boolean reviewed) {
    public static ReservationResponse from(ActivityReservation reservation, boolean reviewed) {
        var activity = reservation.getActivity();
        boolean reviewAllowed = reservation.getActivityStart().plusMinutes(activity.getDurationMinutes())
                .isBefore(LocalDateTime.now());
        return new ReservationResponse(reservation.getId(), activity.getId(), activity.getName(),
                activity.getDepartment(), activity.getCity(), activity.getLocation(), reservation.getActivityStart(),
                activity.getDurationMinutes(), reviewAllowed, reviewed);
    }
}
