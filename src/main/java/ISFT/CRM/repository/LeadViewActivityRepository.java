
package ISFT.CRM.repository;

import ISFT.CRM.entity.LeadViewActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeadViewActivityRepository
        extends JpaRepository<LeadViewActivity, Long> {

    boolean existsByAgentIdAndLeadIdAndViewedDate(
            Long agentId,
            Long leadId,
            LocalDate viewedDate
    );

    List<LeadViewActivity> findByAgentIdAndViewedDate(
            Long agentId,
            LocalDate viewedDate
    );

    List<LeadViewActivity> findByViewedDate(LocalDate viewedDate);
}
