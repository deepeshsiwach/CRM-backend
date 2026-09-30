package ISFT.CRM.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lead_assignments")
public class LeadAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "assigned_at", insertable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    public enum AssignmentStatus {
        ACTIVE,
        INACTIVE
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

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }
}
