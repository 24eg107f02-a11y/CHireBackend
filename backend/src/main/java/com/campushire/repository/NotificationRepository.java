package com.campushire.repository;

import com.campushire.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadStatusFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.readStatus = true where n.user.id = :userId")
    void markAllReadForUser(@Param("userId") Long userId);
}
