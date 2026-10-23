package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.proyTic2026.entities.IndividualProvider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndividualProviderRepository extends JpaRepository<IndividualProvider, Long> {

    boolean existsByCi(String ci);
    boolean existsByCiAndIdNot(String ci, Long id);
    List<IndividualProvider> findAllByStatusOrderByIdAsc(ProviderStatus status);
    Optional<IndividualProvider> findByCi(String ci);

}
