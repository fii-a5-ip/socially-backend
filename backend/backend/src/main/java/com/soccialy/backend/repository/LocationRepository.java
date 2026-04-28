package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {

    Optional<Location> findByName(String name);
    List<Location> findByNameStartingWithIgnoreCase(String prefix);
    List<Location> findByFiltersId(Integer filterId);

    @Query(value = "SELECT l.* FROM locations l LEFT JOIN events e ON l.id = e.location_id GROUP BY l.id ORDER BY COUNT(e.id) DESC", nativeQuery = true)
    List<Location> findTopPopularLocations();
}