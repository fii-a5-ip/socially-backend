package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Filter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FilterRepository extends JpaRepository<Filter, Integer> {
    Optional<Filter> findByName(String name);
    List<Filter> findByNameIn(Collection<String> names);
    boolean existsByName(String name);
    List<Filter> findByCategory(String category);
    List<Filter> findByNameStartingWithIgnoreCase(String prefix);

    @Query(value = """
        SELECT f.* FROM filters f 
        LEFT JOIN user_filters uf ON f.id = uf.filter_id AND uf.user_id = :userId 
        WHERE uf.filter_id IS NULL
        """, nativeQuery = true)
    List<Filter> findFiltersNotSelectedByUser(@Param("userId") Integer userId);

    @Query(value = "SELECT f.* FROM filters f JOIN user_filters uf ON f.id = uf.filter_id GROUP BY f.id ORDER BY COUNT(uf.user_id) DESC LIMIT 5", nativeQuery = true)
    List<Filter> findTop5TrendingFilters();
}
