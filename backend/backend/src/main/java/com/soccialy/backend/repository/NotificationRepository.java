package com.soccialy.backend.repository;

import com.soccialy.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification,Integer>{

    List<Notification>
    findByRecipientUserIdOrderByCreatedAtDesc(
            Integer userId
    );

    long countByRecipientUserIdAndIsReadFalse(
            Integer userId
    );

}
