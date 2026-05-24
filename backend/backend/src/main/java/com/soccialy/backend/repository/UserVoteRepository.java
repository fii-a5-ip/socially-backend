package com.soccialy.backend.repository;

import com.soccialy.backend.entity.UserVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoteRepository extends JpaRepository<UserVote, Integer> {
    Optional<UserVote> findByUserIdAndEventId(Integer userId, Integer eventId);
    int countByEventIdAndVote(Integer eventId, Integer vote);
    
    List<UserVote> findByUserId(Integer userId);
    void deleteByUserIdAndVote(Integer userId, Integer vote);
    void deleteByUserId(Integer userId);
}
