package ISFT.CRM.dto;

import ISFT.CRM.entity.Lead;

public class AgentLeadDetailsUpdateRequest {

    private String fullName;
    private String email;
    private String phone;

    private Integer age;

    private String city;
    private String education;
    private String currentProfession;
    private String primaryObjective;
    private String tradingInvestmentExperience;
    private String customerLookingFor;
    private String interestedArea;

    private Lead.Priority priority;
    private Lead.LeadStatus status;


    // ========================================
    // GETTERS
    // ========================================

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getAge() {
        return age;
    }

    public String getCity() {
        return city;
    }

    public String getEducation() {
        return education;
    }

    public String getCurrentProfession() {
        return currentProfession;
    }

    public String getPrimaryObjective() {
        return primaryObjective;
    }

    public String getTradingInvestmentExperience() {
        return tradingInvestmentExperience;
    }

    public String getCustomerLookingFor() {
        return customerLookingFor;
    }

    public String getInterestedArea() {
        return interestedArea;
    }

    public Lead.Priority getPriority() {
        return priority;
    }

    public Lead.LeadStatus getStatus() {
        return status;
    }


    // ========================================
    // SETTERS
    // ========================================

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public void setCurrentProfession(String currentProfession) {
        this.currentProfession = currentProfession;
    }

    public void setPrimaryObjective(String primaryObjective) {
        this.primaryObjective = primaryObjective;
    }

    public void setTradingInvestmentExperience(
            String tradingInvestmentExperience) {

        this.tradingInvestmentExperience =
                tradingInvestmentExperience;
    }

    public void setCustomerLookingFor(String customerLookingFor) {
        this.customerLookingFor = customerLookingFor;
    }

    public void setInterestedArea(String interestedArea) {
        this.interestedArea = interestedArea;
    }

    public void setPriority(Lead.Priority priority) {
        this.priority = priority;
    }

    public void setStatus(Lead.LeadStatus status) {
        this.status = status;
    }
}