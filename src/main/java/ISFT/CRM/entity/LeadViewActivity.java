
package ISFT.CRM.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lead_view_activities")
public class LeadViewActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Column(name = "viewed_date", nullable = false)
    private LocalDate viewedDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public LeadViewActivity() {
    }

    public LeadViewActivity(Long agentId, Long leadId, LocalDate viewedDate) {
        this.agentId = agentId;
        this.leadId = leadId;
        this.viewedDate = viewedDate;
    }

    public Long getId() {
        return id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public Long getLeadId() {
        return leadId;
    }

    public LocalDate getViewedDate() {
        return viewedDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
