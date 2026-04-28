package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Filter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilterRepository extends JpaRepository<Filter, Integer> {

    Optional<Filter> findByName(String name);
    boolean existsByName(String name);

    @Query(value = "SELECT * FROM filters WHERE id NOT IN (SELECT filter_id FROM user_filters WHERE user_id = ?1)", nativeQuery = true)
    List<Filter> findFiltersNotSelectedByUser(Integer userId);

    @Query(value = "SELECT f.* FROM filters f JOIN user_filters uf ON f.id = uf.filter_id GROUP BY f.id ORDER BY COUNT(uf.user_id) DESC LIMIT 5", nativeQuery = true)
    List<Filter> findTop5TrendingFilters();
}