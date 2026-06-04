package com.soccialy.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.soccialy.backend.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    List<Event> findByLocationId(Integer locationId);
    List<Event> findByNameContainingIgnoreCase(String keyword);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"location", "creator", "group", "participants"})
    List<Event> findByCreatorId(Integer creatorId);
    List<Event> findByGroupId(Integer groupId);
    List<Event> findByScheduledDateAfter(LocalDateTime currentDate);
    List<Event> findByLocationIdNot(Integer excludedLocationId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"location", "creator", "group", "participants"})
    List<Event> findByParticipantsId(Integer userId);
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

    @Query(value = "SELECT e.* FROM events e " +
            "LEFT JOIN event_filters f ON e.id = f.event_id " +
            "WHERE e.date >= :now " +
            "GROUP BY e.id " +
            "ORDER BY MAX(e.name REGEXP :regex OR e.`desc` REGEXP :regex OR f.filter_id IN (:filters)) DESC, e.date ASC " +
            "LIMIT 100",
            nativeQuery = true)
    List<Event> searchByRegexOrFilters(
            @Param("regex") String regex,
            @Param("filters") List<Integer> filters,
            @Param("now") LocalDateTime now
    );

    @Query(value = "SELECT * FROM events WHERE date >= :now ORDER BY date ASC LIMIT 100", nativeQuery = true)
    List<Event> findUpcomingEventsForDiscovery(@Param("now") LocalDateTime now);

    @Query(value = "SELECT * FROM events WHERE date >= :now AND id NOT IN (:votedIds) ORDER BY date ASC LIMIT 100", nativeQuery = true)
    List<Event> findUnvotedUpcomingEvents(@Param("now") LocalDateTime now, @Param("votedIds") List<Integer> votedIds);
}
