package ISFT.CRM.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_logs")
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ========================================
    // LEAD
    // ========================================

    @Column(name = "lead_id", nullable = false)
    private Long leadId;


    // ========================================
    // AGENT
    // ========================================

    @Column(name = "agent_id", nullable = false)
    private Long agentId;


    // ========================================
    // CALL START TIME
    // ========================================

    @Column(name = "call_start_time", nullable = false)
    private LocalDateTime callStartTime;


    // ========================================
    // CALL END TIME
    // ========================================

    @Column(name = "call_end_time")
    private LocalDateTime callEndTime;


    // ========================================
    // OLD DURATION FIELD
    // Kept for existing database records
    // ========================================

    @Column(name = "duration_seconds")
    private Integer durationSeconds;


    // ========================================
    // CALL STATUS
    //
    // Not required anymore for new call logs.
    // Existing records can still contain a status.
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "call_status")
    private CallStatus callStatus;


    // ========================================
    // CALL OUTCOME
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "call_outcome")
    private CallOutcome callOutcome;


    // ========================================
    // REMARKS
    // ========================================

    @Column(columnDefinition = "TEXT")
    private String remarks;


    // ========================================
    // OLD FIELDS
    // Kept for existing records
    // ========================================

    @Column(length = 150)
    private String education;


    @Column(name = "interested_area", length = 255)
    private String interestedArea;


    @Column(length = 150)
    private String city;


    // ========================================
    // CREATED AT
    // ========================================

    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    // ========================================
    // CALL STATUS ENUM
    // Kept for existing backend functionality
    // ========================================

    public enum CallStatus {

        ANSWERED,

        NOT_ANSWERED,

        BUSY,

        FAILED
    }


    // ========================================
    // CALL OUTCOME ENUM
    // ========================================

    public enum CallOutcome {

        INTERESTED,

        NOT_INTERESTED,

        CALL_BACK,

        FOLLOW_UP,

        COUNSELLING,

        ENROLLED,

        WRONG_NUMBER,

        NO_RESPONSE
    }


    // ========================================
    // GET ID
    // ========================================

    public Long getId() {
        return id;
    }


    // ========================================
    // LEAD ID
    // ========================================

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }


    // ========================================
    // AGENT ID
    // ========================================

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }


    // ========================================
    // CALL START TIME
    // ========================================

    public LocalDateTime getCallStartTime() {
        return callStartTime;
    }

    public void setCallStartTime(LocalDateTime callStartTime) {
        this.callStartTime = callStartTime;
    }


    // ========================================
    // CALL END TIME
    // ========================================

    public LocalDateTime getCallEndTime() {
        return callEndTime;
    }

    public void setCallEndTime(LocalDateTime callEndTime) {
        this.callEndTime = callEndTime;
    }


    // ========================================
    // DURATION
    // Kept for existing backend compatibility
    // ========================================

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }


    // ========================================
    // CALL STATUS
    // Nullable now
    // ========================================

    public CallStatus getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(CallStatus callStatus) {
        this.callStatus = callStatus;
    }


    // ========================================
    // CALL OUTCOME
    // ========================================

    public CallOutcome getCallOutcome() {
        return callOutcome;
    }

    public void setCallOutcome(CallOutcome callOutcome) {
        this.callOutcome = callOutcome;
    }


    // ========================================
    // REMARKS
    // ========================================

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }


    // ========================================
    // EDUCATION
    // Kept for existing backend compatibility
    // ========================================

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }


    // ========================================
    // INTERESTED AREA
    // Kept for existing backend compatibility
    // ========================================

    public String getInterestedArea() {
        return interestedArea;
    }

    public void setInterestedArea(String interestedArea) {
        this.interestedArea = interestedArea;
    }


    // ========================================
    // CITY
    // Kept for existing backend compatibility
    // ========================================

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }


    // ========================================
    // CREATED AT
    // ========================================

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}