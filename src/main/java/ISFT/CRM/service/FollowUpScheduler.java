package ISFT.CRM.service;

import ISFT.CRM.entity.FollowUp;
import ISFT.CRM.repository.FollowUpRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FollowUpScheduler {

    private final FollowUpRepository followUpRepository;

    public FollowUpScheduler(
            FollowUpRepository followUpRepository) {

        this.followUpRepository =
                followUpRepository;
    }


    // ============================================================
    // AUTOMATICALLY MARK OVERDUE FOLLOW-UPS AS MISSED
    // ============================================================
    //
    // Runs every 1 minute.
    //
    // PENDING + followUpDate before current time
    //                ↓
    //              MISSED
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
                "Automatic follow-up scheduler: "
                        + overdueFollowUps.size()
                        + " overdue follow-up(s) marked as MISSED."
        );
    }
}