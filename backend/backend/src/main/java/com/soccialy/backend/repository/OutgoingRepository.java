package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    List<Event> findByLocationId(Integer locationId);
    List<Event> findByNameContainingIgnoreCase(String keyword);
    List<Event> findByCreatorId(Integer creatorId);
    List<Event> findByDateAfter(LocalDateTime currentDate);
    List<Event> findByLocationIdNot(Integer excludedLocationId);

    long countByLocationId(Integer locationId);

    @Query(value = "SELECT * FROM events WHERE MATCH(name) AGAINST (?1 IN BOOLEAN MODE)", nativeQuery = true)
    List<Event> searchByNameFullText(String keyword);

    @Query(value = """
        SELECT DISTINCT e.* FROM events e
        JOIN locations l ON e.location_id = l.id
        JOIN location_filters lf ON l.id = lf.location_id
        WHERE lf.filter_id IN (?1)
        """, nativeQuery = true)
    List<Event> findEventsByFilterIds(List<Integer> filterIds);
}