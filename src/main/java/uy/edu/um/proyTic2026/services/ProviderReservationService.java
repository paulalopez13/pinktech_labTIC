package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ProviderReservationResponse;
import uy.edu.um.proyTic2026.repositories.ActivityReservationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderReservationService {
    private final ActivityReservationRepository reservationRepository;

    // Reservas de todas las actividades del prestador, ordenadas por fecha de la actividad.
    @Transactional(readOnly = true)
    public List<ProviderReservationResponse> reservations(Long providerId) {
        return reservationRepository.findAllByActivity_Provider_IdOrderByActivityStartAsc(providerId).stream()
                .map(ProviderReservationResponse::from).toList();
    }
}
