package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {
    Optional<Location> findByName(String name);
    List<Location> findByNameStartingWithIgnoreCase(String prefix);
    List<Location> findByFiltersId(Integer filterId);

    @Query(value = "SELECT l.* FROM locations l LEFT JOIN events e ON l.id = e.location_id GROUP BY l.id ORDER BY COUNT(e.id) DESC", nativeQuery = true)
    List<Location> findTopPopularLocations();

    @Query(value = """
    SELECT *, 
           (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lon)) + 
            sin(radians(:lat)) * sin(radians(latitude)))) AS distance 
    FROM locations 
    WHERE (latitude BETWEEN :lat - (:radius / 111) AND :lat + (:radius / 111))
      AND (longitude BETWEEN :lon - (:radius / (111 * cos(radians(:lat)))) 
                     AND :lon + (:radius / (111 * cos(radians(:lat)))))
    HAVING distance < :radius 
    ORDER BY distance 
    LIMIT 50
    """, nativeQuery = true)
    List<Location> findNearbyLocations(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radius") double radius
    );
}
