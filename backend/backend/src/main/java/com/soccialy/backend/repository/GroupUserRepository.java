package com.soccialy.backend.repository;

import com.soccialy.backend.entity.GroupUser;
import com.soccialy.backend.entity.GroupUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupUserRepository extends JpaRepository<GroupUser, GroupUserId> {

    List<GroupUser> findByGroupId(Integer groupId);

    Optional<GroupUser> findByGroupIdAndUserId(Integer groupId, Integer userId);

    List<GroupUser> findByGroupIdAndRole(Integer groupId, String role);
}