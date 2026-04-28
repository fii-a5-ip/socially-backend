package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {

    Optional<Group> findByName(String name);
    List<Group> findByNameContainingIgnoreCase(String keyword);

    List<Group> findByUsersId(Integer userId);
    List<Group> findByCreatorId(Integer creatorId);

    @Query(value = "SELECT g.* FROM `groups` g LEFT JOIN group_users gu ON g.id = gu.group_id GROUP BY g.id ORDER BY COUNT(gu.user_id) DESC", nativeQuery = true)
    List<Group> findMostPopulatedGroups();
}