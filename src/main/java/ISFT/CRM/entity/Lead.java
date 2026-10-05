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


    @Column(length = 150)
    private String education;


    @Column(name = "interested_area", length = 255)
    private String interestedArea;


    // =========================================================
    // NEW CUSTOMER PROFILE FIELDS
    // =========================================================

    @Column(name = "age")
    private Integer age;


    @Column(name = "current_profession", length = 150)
    private String currentProfession;


    @Column(name = "primary_objective", length = 255)
    private String primaryObjective;


    @Column(
            name = "trading_investment_experience",
            length = 100
    )
    private String tradingInvestmentExperience;


    @Column(name = "customer_looking_for", length = 255)
    private String customerLookingFor;


    @Column(name = "campaign_id")
    private Long campaignId;


    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @Column(
            name = "updated_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;


    // =========================================================
    // ENUMS
    // =========================================================

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


    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

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


    public void setCourseInterested(
            String courseInterested) {

        this.courseInterested =
                courseInterested;
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


    public String getEducation() {

        return education;
    }


    public void setEducation(String education) {

        this.education = education;
    }


    public String getInterestedArea() {

        return interestedArea;
    }


    public void setInterestedArea(
            String interestedArea) {

        this.interestedArea =
                interestedArea;
    }


    // =========================================================
    // NEW CUSTOMER PROFILE GETTERS / SETTERS
    // =========================================================

    public Integer getAge() {

        return age;
    }


    public void setAge(Integer age) {

        this.age = age;
    }


    public String getCurrentProfession() {

        return currentProfession;
    }


    public void setCurrentProfession(
            String currentProfession) {

        this.currentProfession =
                currentProfession;
    }


    public String getPrimaryObjective() {

        return primaryObjective;
    }


    public void setPrimaryObjective(
            String primaryObjective) {

        this.primaryObjective =
                primaryObjective;
    }


    public String getTradingInvestmentExperience() {

        return tradingInvestmentExperience;
    }


    public void setTradingInvestmentExperience(
            String tradingInvestmentExperience) {

        this.tradingInvestmentExperience =
                tradingInvestmentExperience;
    }


    public String getCustomerLookingFor() {

        return customerLookingFor;
    }


    public void setCustomerLookingFor(
            String customerLookingFor) {

        this.customerLookingFor =
                customerLookingFor;
    }


    // =========================================================
    // CAMPAIGN
    // =========================================================

    public Long getCampaignId() {

        return campaignId;
    }


    public void setCampaignId(Long campaignId) {

        this.campaignId = campaignId;
    }


    // =========================================================
    // TIMESTAMPS
    // =========================================================

    public LocalDateTime getCreatedAt() {

        return createdAt;
    }


    public LocalDateTime getUpdatedAt() {

        return updatedAt;
    }

}