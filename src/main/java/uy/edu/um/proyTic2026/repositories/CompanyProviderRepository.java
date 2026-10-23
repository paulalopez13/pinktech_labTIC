package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.proyTic2026.entities.CompanyProvider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyProviderRepository extends JpaRepository<CompanyProvider, Long> {

    boolean existsByRut(String rut);
    boolean existsByRutAndIdNot(String rut, Long id);
    List<CompanyProvider> findAllByStatusOrderByIdAsc(ProviderStatus status);
    Optional<CompanyProvider> findByRut(String rut);

}
