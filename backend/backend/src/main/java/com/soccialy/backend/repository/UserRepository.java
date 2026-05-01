package com.soccialy.backend.repository;

import com.soccialy.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username); //Am adăugat această metodiă fiindcaă SonarQube o caută atunci când face teste. Metoda ar trebui să fie implementată de Spring
}
