package com.soccialy.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.soccialy.backend.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    List<Event> findByLocationId(Integer locationId);

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
