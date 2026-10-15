package com.campushire.controller;

import com.campushire.dto.NotificationResponse;
import com.campushire.security.CampusHireUserDetails;
import com.campushire.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(@AuthenticationPrincipal CampusHireUserDetails principal) {
        return notificationService.getUserNotifications(principal.getUser().getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal CampusHireUserDetails principal) {
        return Map.of("count", notificationService.getUnreadCount(principal.getUser().getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CampusHireUserDetails principal,
            @PathVariable Long id
    ) {
        notificationService.markAsRead(principal.getUser().getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal CampusHireUserDetails principal) {
        notificationService.markAllAsRead(principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
