package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {

    // Află dacă un user a votat deja la un eveniment
    Optional<Vote> findByEventIdAndUserId(Integer eventId, Integer userId);

    // Ia toate voturile pentru un eveniment
    List<Vote> findByEventId(Integer eventId);

    // Numără câte voturi de un anumit fel are un eveniment (ex: câte voturi cu valoarea 1)
    long countByEventIdAndVote(Integer eventId, Integer vote);
}