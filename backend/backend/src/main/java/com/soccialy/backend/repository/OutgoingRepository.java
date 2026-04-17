package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Outgoing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutgoingRepository extends JpaRepository<Outgoing, Integer> {
    List<Outgoing> findByLocationId(Integer locationId);
}
