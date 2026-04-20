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

    // Cautare nume dupa prefix
    List<Location> findByNameStartingWithIgnoreCase(String prefix);

    // Gasește locatii care au un anumit filtru
    List<Location> findByFiltersId(Integer filterId);

    // Sorteaza locatiile in functie de cate iesiri outgoings sunt organizate acolo
    @Query(value = "SELECT l.* FROM locations l LEFT JOIN outgoings o ON l.id = o.location_id GROUP BY l.id ORDER BY COUNT(o.id) DESC", nativeQuery = true)
    List<Location> findTopPopularLocations();
}
