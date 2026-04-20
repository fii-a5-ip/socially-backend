package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Outgoing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutgoingRepository extends JpaRepository<Outgoing, Integer> {

    List<Outgoing> findByLocationId(Integer locationId);
    List<Outgoing> findByNameContainingIgnoreCase(String keyword);

    // Gaseste toate iesirile excluzand o anumita locație de care userul s-a plictisit
    List<Outgoing> findByLocationIdNot(Integer excludedLocationId);

    // Cate evenimente sunt intr-o locatie?
    long countByLocationId(Integer locationId);

    // Full-Text Search
    @Query(value = "SELECT * FROM outgoings WHERE MATCH(name) AGAINST (?1 IN BOOLEAN MODE)", nativeQuery = true)
    List<Outgoing> searchByNameFullText(String keyword);

    // Iesiri bazate pe mai multe filtre
    @Query(value = """
        SELECT DISTINCT o.* FROM outgoings o
        JOIN locations l ON o.location_id = l.id
        JOIN location_filters lf ON l.id = lf.location_id
        WHERE lf.filter_id IN (?1)
        """, nativeQuery = true)
    List<Outgoing> findOutgoingsByFilterIds(List<Integer> filterIds);
}
