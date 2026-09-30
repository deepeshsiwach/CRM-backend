package ISFT.CRM.dto;


import java.util.List;

public class BulkLeadAssignmentRequest {

    private List<Long> leadIds;

    private Long agentId;

    private Long teamId;

    public List<Long> getLeadIds() {
        return leadIds;
    }

    public void setLeadIds(List<Long> leadIds) {
        this.leadIds = leadIds;
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
}