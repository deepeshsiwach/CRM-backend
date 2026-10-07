package ISFT.CRM.service;

import ISFT.CRM.entity.Notification;
import ISFT.CRM.entity.User;
import ISFT.CRM.exception.ResourceNotFoundException;
import ISFT.CRM.repository.NotificationRepository;
import ISFT.CRM.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;


    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.notificationRepository =
                notificationRepository;

        this.userRepository =
                userRepository;
    }


    // ========================================
    // GET CURRENT USER ID
    // ========================================

    private Long getAuthenticatedUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {

            throw new RuntimeException(
                    "Unauthorized access");
        }

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"));
    }


    // ========================================
    // GET ALL NOTIFICATIONS
    // ========================================

    public List<Notification> getMyNotifications() {

        Long userId =
                getAuthenticatedUserId();

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                );
    }


    // ========================================
    // GET UNREAD NOTIFICATIONS
    // ========================================

    public List<Notification> getUnreadNotifications() {

        Long userId =
                getAuthenticatedUserId();

        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                        userId
                );
    }


    // ========================================
    // UNREAD COUNT
    // ========================================

    public long getUnreadCount() {

        Long userId =
                getAuthenticatedUserId();

        return notificationRepository
                .countByUserIdAndReadFalse(
                        userId
                );
    }


    // ========================================
    // MARK ONE AS READ
    // ========================================

    public Notification markAsRead(Long id) {

        Long userId =
                getAuthenticatedUserId();

        Notification notification =
                notificationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Notification not found"));

        if (!notification
                .getUserId()
                .equals(userId)) {

            throw new RuntimeException(
                    "You are not allowed to modify this notification");
        }

        notification.setRead(true);

        return notificationRepository.save(
                notification
        );
    }


    // ========================================
    // MARK ALL AS READ
    // ========================================

    public void markAllAsRead() {

        Long userId =
                getAuthenticatedUserId();

        List<Notification> notifications =
                notificationRepository
                        .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                                userId
                        );

        for (Notification notification :
                notifications) {

            notification.setRead(true);
        }

        notificationRepository.saveAll(
                notifications
        );
    }


    // ========================================
    // MARK UPCOMING FOLLOW-UP NOTIFICATION AS READ
    // ========================================

    public void markUpcomingFollowUpAsRead(
            Long userId,
            Long followUpId) {

        List<Notification> notifications =
                notificationRepository
                        .findByUserIdAndTypeAndReferenceId(
                                userId,
                                "FOLLOW_UP_REMINDER",
                                followUpId
                        );

        for (Notification notification :
                notifications) {

            if (!notification.isRead()) {

                notification.setRead(true);
            }
        }

        notificationRepository.saveAll(
                notifications
        );
    }


    // ========================================
    // CREATE SYSTEM NOTIFICATION
    // ========================================

    public void createNotification(
            Long userId,
            String type,
            String title,
            String message,
            Long referenceId) {

        boolean alreadyExists =
                notificationRepository
                        .existsByUserIdAndTypeAndReferenceId(
                                userId,
                                type,
                                referenceId
                        );

        if (alreadyExists) {
            return;
        }


        Notification notification =
                new Notification();

        notification.setUserId(userId);

        notification.setType(type);

        notification.setTitle(title);

        notification.setMessage(message);

        notification.setReferenceId(referenceId);

        notification.setRead(false);


        notificationRepository.save(
                notification
        );
    }
}