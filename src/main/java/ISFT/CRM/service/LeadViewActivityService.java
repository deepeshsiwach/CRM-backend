
package ISFT.CRM.service;

import ISFT.CRM.entity.LeadViewActivity;
import ISFT.CRM.repository.LeadViewActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class LeadViewActivityService {

    private final LeadViewActivityRepository repository;

    public LeadViewActivityService(LeadViewActivityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordView(Long agentId, Long leadId) {
        LocalDate today = LocalDate.now();

        boolean alreadyViewed =
                repository.existsByAgentIdAndLeadIdAndViewedDate(
                        agentId, leadId, today
                );

        if (!alreadyViewed) {
            repository.save(
                    new LeadViewActivity(agentId, leadId, today)
            );
        }
    }
}
