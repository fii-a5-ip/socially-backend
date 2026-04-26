package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Outgoing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface OutgoingRepository extends JpaRepository<Outgoing, Integer> {
    List<Outgoing> findByLocationId(Integer locationId);

    @Query(value = "SELECT DISTINCT o.* FROM outgoings o " +
            "LEFT JOIN outgoing_filters f ON o.id = f.outgoing_id " +
            "WHERE MATCH(o.name) AGAINST(:searchString IN NATURAL LANGUAGE MODE) " +
            "OR f.filter_id IN (:filters) LIMIT 300",
            nativeQuery = true)
    List<Outgoing> searchByTextOrFilters(
            @Param("searchString") String searchString,
            @Param("filters") List<Integer> filters
    );
}
