package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {

    List<Group> findByUsersId(Integer userId);

    @Query("select g from UserGroup g where lower(g.name) like lower(concat('%', :query, '%'))")
    List<Group> searchByName(@Param("query") String query);
}
