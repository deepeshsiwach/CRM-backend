package ISFT.CRM.controller;

import ISFT.CRM.entity.Lead;
import ISFT.CRM.entity.LeadAssignment;
import ISFT.CRM.service.LeadAssignmentService;
import ISFT.CRM.service.LeadService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentLeadController {

    private final LeadService leadService;
    private final LeadAssignmentService leadAssignmentService;

    public AgentLeadController(
            LeadService leadService,
            LeadAssignmentService leadAssignmentService) {

        this.leadService = leadService;
        this.leadAssignmentService = leadAssignmentService;
    }

    @PutMapping("/leads/{id}/status")
    public Lead updateLeadStatus(
            @PathVariable Long id,
            @RequestBody Lead.LeadStatus status) {

        return leadService.updateLeadStatus(id, status);
    }

    @GetMapping("/leads/{agentId}")
    public List<LeadAssignment> getMyLeads(
            @PathVariable Long agentId) {

        return leadAssignmentService.getActiveAssignmentsByAgent(agentId);
    }
}