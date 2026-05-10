package com.soccialy.backend.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.soccialy.backend.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    List<Event> findByLocationId(Integer locationId);
    List<Event> findByNameContainingIgnoreCase(String keyword);
    List<Event> findByCreatorId(Integer creatorId);
    List<Event> findByScheduledDateAfter(LocalDateTime currentDate);
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

    @Query(value = "SELECT DISTINCT e.* FROM events e " +
            "LEFT JOIN event_filters f ON e.id = f.event_id " +
            "WHERE MATCH(e.name) AGAINST(:searchString IN NATURAL LANGUAGE MODE) " +
            "OR f.filter_id IN (:filters) LIMIT 300",
            nativeQuery = true)
    List<Event> searchByTextOrFilters(
            @Param("searchString") String searchString,
            @Param("filters") List<Integer> filters
    );
}
