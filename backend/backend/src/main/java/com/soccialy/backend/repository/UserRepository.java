package com.soccialy.backend.repository;

import com.soccialy.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByGoogleId(String googleId);

    List<User> findByUsernameStartingWithIgnoreCase(String prefix);
    List<User> findByGroupsId(Integer groupId);
    List<User> findByFiltersId(Integer filterId);
}