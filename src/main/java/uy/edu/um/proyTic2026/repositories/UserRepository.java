package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.proyTic2026.entities.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> { //user es la entidad q administra y long el tipo de id

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
