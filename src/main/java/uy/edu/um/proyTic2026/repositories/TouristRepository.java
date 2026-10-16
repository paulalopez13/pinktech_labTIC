package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.proyTic2026.entities.Tourist;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Long> {
    boolean existsByCi(String ci);
    boolean existsByCiAndIdNot(String ci, Long id);
}
