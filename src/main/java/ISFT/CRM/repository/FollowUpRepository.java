package ISFT.CRM.repository;

import ISFT.CRM.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowUpRepository
        extends JpaRepository<FollowUp, Long> {

    List<FollowUp> findByLeadId(Long leadId);

    void deleteByLeadId(Long leadId);

    List<FollowUp> findByAgentId(Long agentId);

    List<FollowUp> findByStatus(
            FollowUp.FollowUpStatus status
    );

    List<FollowUp> findByAgentIdAndStatus(
            Long agentId,
            FollowUp.FollowUpStatus status
    );

    // ========================================
    // DATE-BASED FOLLOW-UP QUERIES
    // ========================================

    List<FollowUp> findByFollowUpDateBeforeAndStatus(
            LocalDateTime dateTime,
            FollowUp.FollowUpStatus status
    );

    List<FollowUp> findByFollowUpDateBetweenAndStatus(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            FollowUp.FollowUpStatus status
    );

    List<FollowUp> findByFollowUpDateAfterAndStatus(
            LocalDateTime dateTime,
            FollowUp.FollowUpStatus status
    );

    // ========================================
    // AGENT + DATE + STATUS
    // ========================================

    List<FollowUp> findByAgentIdAndFollowUpDateBeforeAndStatus(
            Long agentId,
            LocalDateTime dateTime,
            FollowUp.FollowUpStatus status
    );

    List<FollowUp> findByAgentIdAndFollowUpDateBetweenAndStatus(
            Long agentId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            FollowUp.FollowUpStatus status
    );

    List<FollowUp> findByAgentIdAndFollowUpDateAfterAndStatus(
            Long agentId,
            LocalDateTime dateTime,
            FollowUp.FollowUpStatus status
    );
}