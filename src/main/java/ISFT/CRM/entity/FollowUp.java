package ISFT.CRM.entity;

import jakarta.persistence.*;
        import java.time.LocalDateTime;

@Entity
@Table(name = "follow_ups")
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDateTime followUpDate;

    @Column(length = 255)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpStatus status = FollowUpStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;


    public enum FollowUpStatus {
        PENDING,
        COMPLETED,
        CANCELLED,
        MISSED
    }


    public Long getId() {
        return id;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public LocalDateTime getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDateTime followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public FollowUpStatus getStatus() {
        return status;
    }

    public void setStatus(FollowUpStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}