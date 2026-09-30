package ISFT.CRM.controller;

import ISFT.CRM.dto.BulkLeadAssignmentRequest;
import ISFT.CRM.entity.LeadAssignment;
import ISFT.CRM.service.LeadAssignmentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lead-assignments")
public class LeadAssignmentController {

    private final LeadAssignmentService leadAssignmentService;

    public LeadAssignmentController(
            LeadAssignmentService leadAssignmentService) {

        this.leadAssignmentService =
                leadAssignmentService;
    }


    // ============================================================
    // GET ALL ASSIGNMENTS
    // ============================================================

    @GetMapping
    public List<LeadAssignment> getAllAssignments() {

        return leadAssignmentService.getAllAssignments();
    }


    // ============================================================
    // GET ASSIGNMENTS BY AGENT
    // ============================================================

    @GetMapping("/agent/{agentId}")
    public List<LeadAssignment> getAssignmentsByAgent(
            @PathVariable Long agentId) {

        return leadAssignmentService
                .getAssignmentsByAgent(agentId);
    }


    // ============================================================
    // GET ASSIGNMENTS BY LEAD
    // ============================================================

    @GetMapping("/lead/{leadId}")
    public List<LeadAssignment> getAssignmentsByLead(
            @PathVariable Long leadId) {

        return leadAssignmentService
                .getAssignmentsByLead(leadId);
    }


    // ============================================================
    // GET ACTIVE ASSIGNMENT BY LEAD
    // ============================================================

    @GetMapping("/lead/{leadId}/active")
    public ResponseEntity<LeadAssignment> getActiveAssignmentByLead(
            @PathVariable Long leadId) {

        Optional<LeadAssignment> assignment =
                leadAssignmentService
                        .getActiveAssignmentByLead(leadId);

        return assignment
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    // ============================================================
    // GET ASSIGNMENTS BY TEAM
    // ============================================================

    @GetMapping("/team/{teamId}")
    public List<LeadAssignment> getAssignmentsByTeam(
            @PathVariable Long teamId) {

        return leadAssignmentService
                .getAssignmentsByTeam(teamId);
    }


    // ============================================================
    // GET ASSIGNMENTS BY STATUS
    // ============================================================

    @GetMapping("/status/{status}")
    public List<LeadAssignment> getAssignmentsByStatus(
            @PathVariable LeadAssignment.AssignmentStatus status) {

        return leadAssignmentService
                .getAssignmentsByStatus(status);
    }


    // ============================================================
    // GET ACTIVE ASSIGNMENTS BY AGENT
    // ============================================================

    @GetMapping("/agent/{agentId}/active")
    public List<LeadAssignment> getActiveAssignmentsByAgent(
            @PathVariable Long agentId) {

        return leadAssignmentService
                .getActiveAssignmentsByAgent(agentId);
    }


    // ============================================================
    // GET ACTIVE ASSIGNMENTS BY TEAM
    // ============================================================

    @GetMapping("/team/{teamId}/active")
    public List<LeadAssignment> getActiveAssignmentsByTeam(
            @PathVariable Long teamId) {

        return leadAssignmentService
                .getActiveAssignmentsByTeam(teamId);
    }


    // ============================================================
    // GET ASSIGNMENT BY ID
    // ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<LeadAssignment> getAssignmentById(
            @PathVariable Long id) {

        return leadAssignmentService
                .getAssignmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    // ============================================================
    // CREATE SINGLE ASSIGNMENT
    // ============================================================

    @PostMapping
    public LeadAssignment createAssignment(
            @RequestBody LeadAssignment assignment) {

        return leadAssignmentService
                .createAssignment(assignment);
    }


    // ============================================================
    // BULK LEAD ASSIGNMENT
    // ============================================================

    @PostMapping("/bulk")
    public List<LeadAssignment> bulkAssignLeads(
            @RequestBody BulkLeadAssignmentRequest request) {

        return leadAssignmentService.bulkAssignLeads(
                request.getLeadIds(),
                request.getAgentId(),
                request.getTeamId()
        );
    }


    // ============================================================
    // REASSIGN ACTIVE LEAD
    // ============================================================

    @PutMapping("/{leadId}/reassign")
    public LeadAssignment reassignLead(
            @PathVariable Long leadId,
            @RequestBody ReassignLeadRequest request) {

        return leadAssignmentService.reassignLead(
                leadId,
                request.getNewAgentId(),
                request.getNewTeamId()
        );
    }


    // ============================================================
    // REOPEN CLOSED LEAD AND ASSIGN
    // ============================================================

    @PutMapping("/{leadId}/reopen")
    public LeadAssignment reopenAndAssignLead(
            @PathVariable Long leadId,
            @RequestBody ReassignLeadRequest request) {

        return leadAssignmentService.reopenAndAssignLead(
                leadId,
                request.getNewAgentId(),
                request.getNewTeamId()
        );
    }


    // ============================================================
    // UPDATE ASSIGNMENT STATUS
    // ============================================================

    @PutMapping("/{id}/status")
    public LeadAssignment updateAssignmentStatus(
            @PathVariable Long id,
            @RequestParam LeadAssignment.AssignmentStatus status) {

        return leadAssignmentService
                .updateAssignmentStatus(
                        id,
                        status);
    }


    // ============================================================
    // DELETE ASSIGNMENT
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long id) {

        leadAssignmentService
                .deleteAssignment(id);

        return ResponseEntity
                .noContent()
                .build();
    }


    // ============================================================
    // REASSIGN / REOPEN REQUEST DTO
    // ============================================================

    public static class ReassignLeadRequest {

        private Long newAgentId;

        private Long newTeamId;


        public Long getNewAgentId() {

            return newAgentId;
        }


        public void setNewAgentId(Long newAgentId) {

            this.newAgentId = newAgentId;
        }


        public Long getNewTeamId() {

            return newTeamId;
        }


        public void setNewTeamId(Long newTeamId) {

            this.newTeamId = newTeamId;
        }
    }
}