package ISFT.CRM.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_logs")
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "call_start_time", nullable = false)
    private LocalDateTime callStartTime;

    @Column(name = "call_end_time")
    private LocalDateTime callEndTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_status", nullable = false)
    private CallStatus callStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_outcome")
    private CallOutcome callOutcome;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(length = 150)
    private String education;

    @Column(name = "interested_area", length = 255)
    private String interestedArea;

    @Column(length = 150)
    private String city;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;


    public enum CallStatus {
        ANSWERED,
        NOT_ANSWERED,
        BUSY,
        FAILED
    }


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


    public LocalDateTime getCallStartTime() {
        return callStartTime;
    }

    public void setCallStartTime(LocalDateTime callStartTime) {
        this.callStartTime = callStartTime;
    }


    public LocalDateTime getCallEndTime() {
        return callEndTime;
    }

    public void setCallEndTime(LocalDateTime callEndTime) {
        this.callEndTime = callEndTime;
    }


    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }


    public CallStatus getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(CallStatus callStatus) {
        this.callStatus = callStatus;
    }


    public CallOutcome getCallOutcome() {
        return callOutcome;
    }

    public void setCallOutcome(CallOutcome callOutcome) {
        this.callOutcome = callOutcome;
    }


    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }


    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }


    public String getInterestedArea() {
        return interestedArea;
    }

    public void setInterestedArea(String interestedArea) {
        this.interestedArea = interestedArea;
    }


    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}