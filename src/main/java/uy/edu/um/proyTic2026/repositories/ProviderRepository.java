package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.proyTic2026.entities.Provider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;

import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    // Trae empresas y prestadores particulares juntos, porque ambos heredan de Provider.
    List<Provider> findAllByStatusOrderByIdAsc(ProviderStatus status);
}
