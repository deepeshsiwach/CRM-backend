package ISFT.CRM.service;

import ISFT.CRM.entity.FollowUp;
import ISFT.CRM.repository.FollowUpRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FollowUpScheduler {

    private final FollowUpRepository followUpRepository;

    public FollowUpScheduler(
            FollowUpRepository followUpRepository) {

        this.followUpRepository = followUpRepository;
    }


    // ============================================================
    // AUTOMATICALLY MARK OVERDUE FOLLOW-UPS AS MISSED
    // ============================================================
    //
    // IMPORTANT:
    // Automatic scheduling has been removed from this class.
    //
    // FollowUpReminderScheduler is now responsible for:
    //
    // 1. Upcoming follow-up notifications
    // 2. Overdue follow-up notifications
    // 3. Marking overdue follow-ups as MISSED
    //
    // This method is kept for compatibility and can still be
    // called manually if needed.
    //
    // ============================================================

    @Transactional
    public void markOverdueFollowUpsAsMissed() {

        LocalDateTime now =
                LocalDateTime.now();

        List<FollowUp> overdueFollowUps =
                followUpRepository
                        .findByFollowUpDateBeforeAndStatus(
                                now,
                                FollowUp.FollowUpStatus.PENDING
                        );


        if (overdueFollowUps.isEmpty()) {

            return;
        }


        for (FollowUp followUp :
                overdueFollowUps) {

            followUp.setStatus(
                    FollowUp.FollowUpStatus.MISSED
            );
        }


        followUpRepository.saveAll(
                overdueFollowUps
        );


        System.out.println(
                "Manual follow-up scheduler: "
                        + overdueFollowUps.size()
                        + " overdue follow-up(s) marked as MISSED."
        );
    }
}