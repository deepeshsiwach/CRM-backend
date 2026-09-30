package ISFT.CRM.entity;

import jakarta.persistence.*;

        import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "course_interested", length = 100)
    private String courseInterested;

    @Column(name = "lead_source", length = 50)
    private String leadSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(length = 100)
    private String city;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;


    public enum LeadStatus {
        NEW,
        CONTACTED,
        INTERESTED,
        FOLLOW_UP,
        COUNSELLING,
        ENROLLED,
        NOT_INTERESTED,
        WRONG_NUMBER,
        NO_RESPONSE,
        LOST
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCourseInterested() {
        return courseInterested;
    }

    public void setCourseInterested(String courseInterested) {
        this.courseInterested = courseInterested;
    }

    public String getLeadSource() {
        return leadSource;
    }

    public void setLeadSource(String leadSource) {
        this.leadSource = leadSource;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}