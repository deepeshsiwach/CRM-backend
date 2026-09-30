package ISFT.CRM.controller;

import ISFT.CRM.entity.Notification;
import ISFT.CRM.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;


    public NotificationController(
            NotificationService notificationService) {

        this.notificationService =
                notificationService;
    }


    // ========================================
    // ALL NOTIFICATIONS
    // ========================================

    @GetMapping
    public List<Notification> getMyNotifications() {

        return notificationService
                .getMyNotifications();
    }


    // ========================================
    // UNREAD NOTIFICATIONS
    // ========================================

    @GetMapping("/unread")
    public List<Notification> getUnreadNotifications() {

        return notificationService
                .getUnreadNotifications();
    }


    // ========================================
    // UNREAD COUNT
    // ========================================

    @GetMapping("/unread/count")
    public long getUnreadCount() {

        return notificationService
                .getUnreadCount();
    }


    // ========================================
    // MARK ONE AS READ
    // ========================================

    @PutMapping("/{id}/read")
    public Notification markAsRead(
            @PathVariable Long id) {

        return notificationService
                .markAsRead(id);
    }


    // ========================================
    // MARK ALL AS READ
    // ========================================

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {

        notificationService.markAllAsRead();

        return ResponseEntity.noContent().build();
    }
}