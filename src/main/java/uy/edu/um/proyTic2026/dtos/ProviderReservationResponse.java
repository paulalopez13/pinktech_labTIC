package uy.edu.um.proyTic2026.dtos;

import uy.edu.um.proyTic2026.entities.ActivityReservation;
import uy.edu.um.proyTic2026.entities.ReservationParticipant;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

// Una reserva vista por el prestador: quién reservó, para cuándo y los datos de cada participante.
public record ProviderReservationResponse(Long id, Long activityId, String activityName, LocalDateTime activityStart,
                                          Instant reservedAt, String paymentStatus, int participantCount,
                                          BigDecimal total, String contactName, String contactEmail,
                                          String contactPhone, List<Participant> participants) {

    public record Participant(String fullName, String documentType, String documentNumber, Integer age,
                              Integer heightCm, String level) {
        static Participant from(ReservationParticipant participant) {
            return new Participant(participant.getFullName(),
                    participant.getDocumentType() == null ? null : participant.getDocumentType().name(),
                    participant.getDocumentNumber(), participant.getAge(), participant.getHeightCm(),
                    participant.getLevel());
        }
    }

    public static ProviderReservationResponse from(ActivityReservation reservation) {
        var activity = reservation.getActivity();
        int count = reservation.getParticipantCount() == null ? 1 : reservation.getParticipantCount();
        return new ProviderReservationResponse(reservation.getId(), activity.getId(), activity.getName(),
                reservation.getActivityStart(), reservation.getReservedAt(),
                reservation.getPaymentStatus() == null ? "APPROVED" : reservation.getPaymentStatus().name(),
                count, activity.getPrice().multiply(BigDecimal.valueOf(count)),
                reservation.getContactName(), reservation.getContactEmail(), reservation.getContactPhone(),
                reservation.getParticipants().stream().map(Participant::from).toList());
    }
}
