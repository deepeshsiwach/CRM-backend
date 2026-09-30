package ISFT.CRM.service;

import ISFT.CRM.entity.FollowUp;
import ISFT.CRM.repository.FollowUpRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FollowUpReminderScheduler {

    private final FollowUpRepository followUpRepository;
    private final NotificationService notificationService;


    public FollowUpReminderScheduler(
            FollowUpRepository followUpRepository,
            NotificationService notificationService) {

        this.followUpRepository =
                followUpRepository;

        this.notificationService =
                notificationService;
    }


    // ========================================
    // RUN EVERY 1 MINUTE
    // ========================================

    @Scheduled(fixedRate = 60000)
    public void processFollowUpReminders() {

        LocalDateTime now =
                LocalDateTime.now();


        // ========================================
        // UPCOMING FOLLOW-UPS
        // NEXT 15 MINUTES
        // ========================================

        LocalDateTime reminderEnd =
                now.plusMinutes(15);

        List<FollowUp> upcomingFollowUps =
                followUpRepository
                        .findByFollowUpDateBetweenAndStatus(
                                now,
                                reminderEnd,
                                FollowUp.FollowUpStatus.PENDING
                        );


        for (FollowUp followUp :
                upcomingFollowUps) {

            notificationService.createNotification(

                    followUp.getAgentId(),

                    "FOLLOW_UP_REMINDER",

                    "Upcoming Follow-up",

                    "Follow-up #" +
                            followUp.getId() +
                            " is scheduled for " +
                            followUp.getFollowUpDate() +
                            ". Purpose: " +
                            (followUp.getPurpose() == null
                                    ? "Follow-up"
                                    : followUp.getPurpose()),

                    followUp.getId()
            );
        }


        // ========================================
        // OVERDUE FOLLOW-UPS
        // ========================================

        List<FollowUp> overdueFollowUps =
                followUpRepository
                        .findByFollowUpDateBeforeAndStatus(
                                now,
                                FollowUp.FollowUpStatus.PENDING
                        );


        for (FollowUp followUp :
                overdueFollowUps) {


            // ========================================
            // CREATE OVERDUE NOTIFICATION
            // ========================================

            notificationService.createNotification(

                    followUp.getAgentId(),

                    "FOLLOW_UP_OVERDUE",

                    "Overdue Follow-up",

                    "Follow-up #" +
                            followUp.getId() +
                            " is overdue. Please complete it.",

                    followUp.getId()
            );


            // ========================================
            // MARK AS MISSED
            // ========================================

            followUp.setStatus(
                    FollowUp.FollowUpStatus.MISSED
            );

            followUpRepository.save(
                    followUp
            );
        }
    }
}