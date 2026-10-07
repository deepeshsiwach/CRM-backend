package ISFT.CRM.service;

import ISFT.CRM.entity.FollowUp;
import ISFT.CRM.repository.FollowUpRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class FollowUpReminderScheduler {

    private final FollowUpRepository followUpRepository;
    private final NotificationService notificationService;

    // India timezone
    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");


    public FollowUpReminderScheduler(
            FollowUpRepository followUpRepository,
            NotificationService notificationService) {

        this.followUpRepository = followUpRepository;
        this.notificationService = notificationService;
    }


    // ========================================
    // RUN EVERY 1 MINUTE
    // ========================================

    @Scheduled(fixedRate = 60000)
    public void processFollowUpReminders() {

        // IMPORTANT:
        // Always use India time for CRM follow-ups
        LocalDateTime now =
                LocalDateTime.now(INDIA_ZONE);


        System.out.println(
                "Follow-up reminder scheduler running at India time: "
                        + now
        );


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

                    "Follow-up #"
                            + followUp.getId()
                            + " is scheduled for "
                            + followUp.getFollowUpDate()
                            + ". Purpose: "
                            + (
                            followUp.getPurpose() == null
                                    ? "Follow-up"
                                    : followUp.getPurpose()
                    ),

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
            // MARK OLD UPCOMING NOTIFICATION AS READ
            // ========================================

            notificationService.markUpcomingFollowUpAsRead(
                    followUp.getAgentId(),
                    followUp.getId()
            );


            // ========================================
            // CREATE OVERDUE NOTIFICATION
            // ========================================

            notificationService.createNotification(

                    followUp.getAgentId(),

                    "FOLLOW_UP_OVERDUE",

                    "Overdue Follow-up",

                    "Follow-up #"
                            + followUp.getId()
                            + " is overdue. Please complete it.",

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