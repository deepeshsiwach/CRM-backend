package ISFT.CRM.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "agent_attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_agent_attendance_date",
                        columnNames = {
                                "agent_id",
                                "attendance_date"
                        }
                )
        }
)
public class AgentAttendance {

    // ========================================
    // ID
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ========================================
    // AGENT ID
    // ========================================

    @Column(name = "agent_id", nullable = false)
    private Long agentId;


    // ========================================
    // ATTENDANCE DATE
    // ========================================

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;


    // ========================================
    // LOGIN TIME
    // ========================================

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;


    // ========================================
    // LOGOUT TIME
    // ========================================

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;


    // ========================================
    // CREATED AT
    // ========================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    // ========================================
    // UPDATED AT
    // ========================================

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // ========================================
    // PRE-PERSIST
    // ========================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

    }


    // ========================================
    // PRE-UPDATE
    // ========================================

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();

    }


    // ========================================
    // GET ID
    // ========================================

    public Long getId() {
        return id;
    }


    // ========================================
    // GET AGENT ID
    // ========================================

    public Long getAgentId() {
        return agentId;
    }


    // ========================================
    // SET AGENT ID
    // ========================================

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }


    // ========================================
    // GET ATTENDANCE DATE
    // ========================================

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }


    // ========================================
    // SET ATTENDANCE DATE
    // ========================================

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }


    // ========================================
    // GET LOGIN TIME
    // ========================================

    public LocalDateTime getLoginTime() {
        return loginTime;
    }


    // ========================================
    // SET LOGIN TIME
    // ========================================

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }


    // ========================================
    // GET LOGOUT TIME
    // ========================================

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }


    // ========================================
    // SET LOGOUT TIME
    // ========================================

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }


    // ========================================
    // GET CREATED AT
    // ========================================

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    // ========================================
    // GET UPDATED AT
    // ========================================

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}