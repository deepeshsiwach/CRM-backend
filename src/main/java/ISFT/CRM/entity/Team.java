package ISFT.CRM.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamStatus status = TeamStatus.ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum TeamStatus {
        ACTIVE,
        INACTIVE
    }

    public Long getId() {
        return id;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TeamStatus getStatus() {
        return status;
    }

    public void setStatus(TeamStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}