package com.socially.repository;

import com.socially.entity.Notification;
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

    long countByRecipientUserIdAndReadFalse(
            Integer userId
    );

}