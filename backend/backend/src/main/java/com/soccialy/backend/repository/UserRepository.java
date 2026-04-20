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

    //Cautare nume dupa prefix
    List<User> findByUsernameStartingWithIgnoreCase(String prefix);

    //Gaseste toti utilizatorii care fac parte dintr-un anumit grup
    List<User> findByGroupsId(Integer groupId);

    //Gaseste toti utilizatorii care au bifat un anumit filtru
    List<User> findByFiltersId(Integer filterId);
}