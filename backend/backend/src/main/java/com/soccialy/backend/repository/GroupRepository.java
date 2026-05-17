package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
    Optional<Group> findByName(String name);
    List<Group> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT gu.group FROM GroupUser gu WHERE gu.user.id = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Integer userId);

    List<Group> findByCreatorId(Integer creatorId);

    @Query("SELECT g FROM UserGroup g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(g.desc) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Group> searchGroups(@Param("keyword") String keyword);

    @Query(value = """
        SELECT g.* FROM `groups` g 
        LEFT JOIN group_users gu ON g.id = gu.group_id 
        GROUP BY g.id 
        ORDER BY COUNT(gu.user_id) DESC 
        LIMIT 50
        """, nativeQuery = true)
    List<Group> findTopPopulatedGroups();

    @Query(value = "SELECT COUNT(*) FROM group_users WHERE group_id = ?1", nativeQuery = true)
    long countMembers(Integer groupId);
}
