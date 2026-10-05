package ISFT.CRM.service;

import ISFT.CRM.entity.Lead;
import ISFT.CRM.entity.LeadAssignment;
import ISFT.CRM.entity.User;
import ISFT.CRM.entity.Team;

import ISFT.CRM.repository.LeadAssignmentRepository;
import ISFT.CRM.repository.LeadRepository;
import ISFT.CRM.repository.UserRepository;
import ISFT.CRM.repository.TeamRepository;

import ISFT.CRM.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
public class LeadAssignmentService {

    private final LeadAssignmentRepository leadAssignmentRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;


    public LeadAssignmentService(
            LeadAssignmentRepository leadAssignmentRepository,
            LeadRepository leadRepository,
            UserRepository userRepository,
            TeamRepository teamRepository,
            NotificationService notificationService) {

        this.leadAssignmentRepository = leadAssignmentRepository;
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.notificationService = notificationService;
    }


    // ============================================================
    // GET ALL ASSIGNMENTS
    // ============================================================

    public List<LeadAssignment> getAllAssignments() {

        return leadAssignmentRepository.findAll();
    }


    // ============================================================
    // GET ASSIGNMENTS BY AGENT
    // ============================================================

    public List<LeadAssignment> getAssignmentsByAgent(
            Long agentId) {

        return leadAssignmentRepository.findByAgentId(agentId);
    }


    // ============================================================
    // GET ASSIGNMENTS BY LEAD
    // ============================================================

    public List<LeadAssignment> getAssignmentsByLead(
            Long leadId) {

        return leadAssignmentRepository.findByLeadId(leadId);
    }


    // ============================================================
    // GET ACTIVE ASSIGNMENT BY LEAD
    // ============================================================

    public Optional<LeadAssignment> getActiveAssignmentByLead(
            Long leadId) {

        return leadAssignmentRepository.findByLeadIdAndStatus(
                leadId,
                LeadAssignment.AssignmentStatus.ACTIVE
        );
    }


    // ============================================================
    // GET ASSIGNMENTS BY TEAM
    // ============================================================

    public List<LeadAssignment> getAssignmentsByTeam(
            Long teamId) {

        return leadAssignmentRepository.findByTeamId(teamId);
    }


    // ============================================================
    // GET ASSIGNMENTS BY STATUS
    // ============================================================

    public List<LeadAssignment> getAssignmentsByStatus(
            LeadAssignment.AssignmentStatus status) {

        return leadAssignmentRepository.findByStatus(status);
    }


    // ============================================================
    // GET ACTIVE ASSIGNMENTS BY AGENT
    // ============================================================

    public List<LeadAssignment> getActiveAssignmentsByAgent(
            Long agentId) {

        return leadAssignmentRepository.findByAgentIdAndStatus(
                agentId,
                LeadAssignment.AssignmentStatus.ACTIVE
        );
    }


    // ============================================================
    // GET ACTIVE ASSIGNMENTS BY TEAM
    // ============================================================

    public List<LeadAssignment> getActiveAssignmentsByTeam(
            Long teamId) {

        return leadAssignmentRepository.findByTeamIdAndStatus(
                teamId,
                LeadAssignment.AssignmentStatus.ACTIVE
        );
    }


    // ============================================================
    // GET ASSIGNMENT BY ID
    // ============================================================

    public Optional<LeadAssignment> getAssignmentById(
            Long id) {

        return leadAssignmentRepository.findById(id);
    }


    // ============================================================
    // CREATE SINGLE ASSIGNMENT
    // ============================================================

    public LeadAssignment createAssignment(
            LeadAssignment assignment) {

        if (assignment.getLeadId() == null) {

            throw new IllegalArgumentException(
                    "Lead is required");
        }

        if (assignment.getAgentId() == null) {

            throw new IllegalArgumentException(
                    "Agent is required");
        }


        // --------------------------------------------------------
        // CHECK LEAD
        // --------------------------------------------------------

        Lead lead =
                leadRepository.findById(
                        assignment.getLeadId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lead not found"));


        // --------------------------------------------------------
        // CLOSED LEADS CANNOT BE ASSIGNED
        // --------------------------------------------------------

        if (isClosedLead(lead)) {

            throw new IllegalArgumentException(
                    "Closed lead cannot be assigned. Reopen the lead first.");
        }


        // --------------------------------------------------------
        // CHECK EXISTING ACTIVE ASSIGNMENT
        // --------------------------------------------------------

        if (leadAssignmentRepository
                .findByLeadIdAndStatus(
                        assignment.getLeadId(),
                        LeadAssignment.AssignmentStatus.ACTIVE
                )
                .isPresent()) {

            throw new RuntimeException(
                    "Lead already has an active assignment");
        }


        // --------------------------------------------------------
        // CHECK AGENT
        // --------------------------------------------------------

        User agent =
                userRepository.findById(
                        assignment.getAgentId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agent not found"));


        if (agent.getRole() != User.Role.AGENT) {

            throw new RuntimeException(
                    "Selected user is not an agent");
        }


        if (agent.getStatus() != User.Status.ACTIVE) {

            throw new RuntimeException(
                    "Agent is inactive");
        }


        // --------------------------------------------------------
        // CHECK TEAM
        // --------------------------------------------------------

        if (assignment.getTeamId() != null) {

            Team team =
                    teamRepository.findById(
                            assignment.getTeamId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Team not found"));


            if (team.getStatus() != Team.TeamStatus.ACTIVE) {

                throw new RuntimeException(
                        "Team is inactive");
            }
        }


        // --------------------------------------------------------
        // FORCE ACTIVE STATUS
        // --------------------------------------------------------

        assignment.setStatus(
                LeadAssignment.AssignmentStatus.ACTIVE
        );


        return leadAssignmentRepository.save(
                assignment);
    }


    // ============================================================
    // REASSIGN / TRANSFER SINGLE LEAD
    //
    // FLOW:
    //
    // OLD AGENT
    //      ↓
    // OLD ASSIGNMENT = INACTIVE
    //      ↓
    // NEW AGENT + NEW TEAM
    //      ↓
    // NEW ASSIGNMENT = ACTIVE
    //      ↓
    // NOTIFICATION TO NEW AGENT
    // ============================================================

    @Transactional
    public LeadAssignment reassignLead(
            Long leadId,
            Long newAgentId,
            Long newTeamId) {


        // --------------------------------------------------------
        // CHECK LEAD
        // --------------------------------------------------------

        Lead lead =
                leadRepository.findById(leadId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Lead not found"));


        // --------------------------------------------------------
        // CLOSED LEADS CANNOT BE REASSIGNED
        // --------------------------------------------------------

        if (isClosedLead(lead)) {

            throw new IllegalArgumentException(
                    "Closed lead cannot be reassigned. Reopen the lead first.");
        }


        // --------------------------------------------------------
        // CHECK NEW AGENT
        // --------------------------------------------------------

        if (newAgentId == null) {

            throw new IllegalArgumentException(
                    "New agent is required");
        }


        User newAgent =
                userRepository.findById(newAgentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agent not found"));


        if (newAgent.getRole() != User.Role.AGENT) {

            throw new RuntimeException(
                    "Selected user is not an agent");
        }


        if (newAgent.getStatus() != User.Status.ACTIVE) {

            throw new RuntimeException(
                    "Agent is inactive");
        }


        // --------------------------------------------------------
        // CHECK NEW TEAM
        // --------------------------------------------------------

        Team newTeam = null;

        if (newTeamId != null) {

            newTeam =
                    teamRepository.findById(newTeamId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Team not found"));


            if (newTeam.getStatus()
                    != Team.TeamStatus.ACTIVE) {

                throw new RuntimeException(
                        "Team is inactive");
            }
        }


        // --------------------------------------------------------
        // GET CURRENT ACTIVE ASSIGNMENT
        // --------------------------------------------------------

        Optional<LeadAssignment> currentAssignment =
                leadAssignmentRepository
                        .findByLeadIdAndStatus(
                                leadId,
                                LeadAssignment.AssignmentStatus.ACTIVE
                        );


        // --------------------------------------------------------
        // IF SAME AGENT + SAME TEAM
        // NO NEED TO TRANSFER
        // --------------------------------------------------------

        if (currentAssignment.isPresent()) {

            LeadAssignment oldAssignment =
                    currentAssignment.get();


            if (oldAssignment.getAgentId()
                    .equals(newAgentId)
                    &&
                    (
                            (oldAssignment.getTeamId() == null
                                    && newTeamId == null)
                                    ||
                                    (oldAssignment.getTeamId() != null
                                            && oldAssignment.getTeamId()
                                            .equals(newTeamId))
                    )
            ) {

                return oldAssignment;
            }
        }


        // --------------------------------------------------------
        // STORE OLD ASSIGNMENT INFORMATION
        // --------------------------------------------------------

        Long oldAgentId = null;
        Long oldTeamId = null;

        if (currentAssignment.isPresent()) {

            LeadAssignment oldAssignment =
                    currentAssignment.get();

            oldAgentId = oldAssignment.getAgentId();
            oldTeamId = oldAssignment.getTeamId();
        }


        // --------------------------------------------------------
        // DEACTIVATE OLD ASSIGNMENT
        // --------------------------------------------------------

        if (currentAssignment.isPresent()) {

            LeadAssignment oldAssignment =
                    currentAssignment.get();

            oldAssignment.setStatus(
                    LeadAssignment.AssignmentStatus.INACTIVE
            );

            leadAssignmentRepository.save(
                    oldAssignment);
        }


        // --------------------------------------------------------
        // CREATE NEW ACTIVE ASSIGNMENT
        // --------------------------------------------------------

        LeadAssignment newAssignment =
                new LeadAssignment();

        newAssignment.setLeadId(
                leadId);

        newAssignment.setAgentId(
                newAgentId);

        newAssignment.setTeamId(
                newTeamId);

        newAssignment.setStatus(
                LeadAssignment.AssignmentStatus.ACTIVE);


        LeadAssignment savedAssignment =
                leadAssignmentRepository.save(
                        newAssignment);


        // --------------------------------------------------------
        // CREATE NOTIFICATION FOR NEW AGENT
        // --------------------------------------------------------

        String teamText;

        if (newTeam != null) {

            teamText =
                    " Team ID: " + newTeam.getId();

        } else {

            teamText =
                    "";
        }


        String notificationMessage =
                "Lead #" + leadId
                        + " has been transferred to you."
                        + teamText;


        notificationService.createNotification(
                newAgentId,
                "LEAD_TRANSFERRED",
                "Lead Transferred",
                notificationMessage,
                savedAssignment.getId()
        );


        return savedAssignment;
    }


    // ============================================================
    // REOPEN CLOSED LEAD AND ASSIGN
    // ============================================================

    @Transactional
    public LeadAssignment reopenAndAssignLead(
            Long leadId,
            Long newAgentId,
            Long newTeamId) {


        // --------------------------------------------------------
        // CHECK LEAD ID
        // --------------------------------------------------------

        if (leadId == null) {

            throw new IllegalArgumentException(
                    "Lead ID is required");
        }


        // --------------------------------------------------------
        // CHECK AGENT ID
        // --------------------------------------------------------

        if (newAgentId == null) {

            throw new IllegalArgumentException(
                    "New agent is required");
        }


        // --------------------------------------------------------
        // FIND LEAD
        // --------------------------------------------------------

        Lead lead =
                leadRepository.findById(
                        leadId
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lead not found"));


        // --------------------------------------------------------
        // LEAD MUST BE CLOSED
        // --------------------------------------------------------

        if (!isClosedLead(lead)) {

            throw new IllegalArgumentException(
                    "Lead is not closed and does not need reopening.");
        }


        // --------------------------------------------------------
        // VALIDATE NEW AGENT
        // --------------------------------------------------------

        User newAgent =
                userRepository.findById(
                        newAgentId
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agent not found"));


        if (newAgent.getRole() != User.Role.AGENT) {

            throw new IllegalArgumentException(
                    "Selected user is not an agent");
        }


        if (newAgent.getStatus() != User.Status.ACTIVE) {

            throw new IllegalArgumentException(
                    "Agent is inactive");
        }


        // --------------------------------------------------------
        // VALIDATE NEW TEAM
        // --------------------------------------------------------

        if (newTeamId != null) {

            Team team =
                    teamRepository.findById(
                            newTeamId
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Team not found"));


            if (team.getStatus()
                    != Team.TeamStatus.ACTIVE) {

                throw new IllegalArgumentException(
                        "Team is inactive");
            }
        }


        // --------------------------------------------------------
        // DEACTIVATE ANY OLD ACTIVE ASSIGNMENT
        // --------------------------------------------------------

        Optional<LeadAssignment> existingActiveAssignment =
                leadAssignmentRepository
                        .findByLeadIdAndStatus(
                                leadId,
                                LeadAssignment.AssignmentStatus.ACTIVE
                        );


        if (existingActiveAssignment.isPresent()) {

            LeadAssignment oldAssignment =
                    existingActiveAssignment.get();

            oldAssignment.setStatus(
                    LeadAssignment.AssignmentStatus.INACTIVE
            );

            leadAssignmentRepository.save(
                    oldAssignment);
        }


        // --------------------------------------------------------
        // REOPEN LEAD
        // --------------------------------------------------------

        lead.setStatus(
                Lead.LeadStatus.NEW
        );

        leadRepository.save(lead);


        // --------------------------------------------------------
        // CREATE NEW ACTIVE ASSIGNMENT
        // --------------------------------------------------------

        LeadAssignment newAssignment =
                new LeadAssignment();

        newAssignment.setLeadId(
                leadId);

        newAssignment.setAgentId(
                newAgentId);

        newAssignment.setTeamId(
                newTeamId);

        newAssignment.setStatus(
                LeadAssignment.AssignmentStatus.ACTIVE);


        return leadAssignmentRepository.save(
                newAssignment);
    }


    // ============================================================
    // BULK LEAD ASSIGNMENT
    // ============================================================

    @Transactional
    public List<LeadAssignment> bulkAssignLeads(
            List<Long> leadIds,
            Long agentId,
            Long teamId) {

        if (leadIds == null ||
                leadIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one lead must be selected");
        }


        if (agentId == null) {

            throw new IllegalArgumentException(
                    "Agent is required");
        }


        // --------------------------------------------------------
        // CHECK AGENT
        // --------------------------------------------------------

        User agent =
                userRepository.findById(agentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agent not found"));


        if (agent.getRole() != User.Role.AGENT) {

            throw new IllegalArgumentException(
                    "Selected user is not an agent");
        }


        if (agent.getStatus() != User.Status.ACTIVE) {

            throw new IllegalArgumentException(
                    "Selected agent is inactive");
        }


        // --------------------------------------------------------
        // CHECK TEAM
        // --------------------------------------------------------

        if (teamId != null) {

            Team team =
                    teamRepository.findById(teamId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Team not found"));


            if (team.getStatus()
                    != Team.TeamStatus.ACTIVE) {

                throw new IllegalArgumentException(
                        "Team is inactive");
            }
        }


        List<LeadAssignment> assignments =
                new ArrayList<>();


        // --------------------------------------------------------
        // PROCESS EACH LEAD
        // --------------------------------------------------------

        for (Long leadId : leadIds) {

            if (leadId == null) {
                continue;
            }


            // Check lead exists

            Lead lead =
                    leadRepository.findById(leadId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Lead not found: "
                                                    + leadId));


            // ----------------------------------------------------
            // CLOSED LEAD
            // ----------------------------------------------------

            if (isClosedLead(lead)) {

                throw new IllegalArgumentException(
                        "Lead " + leadId
                                + " is closed. Reopen the lead first.");
            }


            // ----------------------------------------------------
            // CHECK ACTIVE ASSIGNMENT
            // ----------------------------------------------------

            Optional<LeadAssignment> existingAssignment =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    leadId,
                                    LeadAssignment.AssignmentStatus.ACTIVE
                            );


            // ----------------------------------------------------
            // ALREADY ASSIGNED -> SKIP
            // ----------------------------------------------------

            if (existingAssignment.isPresent()) {

                continue;
            }


            // ----------------------------------------------------
            // CREATE ASSIGNMENT
            // ----------------------------------------------------

            LeadAssignment assignment =
                    new LeadAssignment();

            assignment.setLeadId(
                    leadId);

            assignment.setAgentId(
                    agentId);

            assignment.setTeamId(
                    teamId);

            assignment.setStatus(
                    LeadAssignment.AssignmentStatus.ACTIVE);

            assignments.add(
                    assignment);
        }


        // --------------------------------------------------------
        // NOTHING TO ASSIGN
        // --------------------------------------------------------

        if (assignments.isEmpty()) {

            throw new IllegalArgumentException(
                    "All selected leads are already assigned");
        }


        // --------------------------------------------------------
        // SAVE ALL
        // --------------------------------------------------------

        return leadAssignmentRepository.saveAll(
                assignments);
    }


    // ============================================================
    // UPDATE ASSIGNMENT STATUS
    // ============================================================

    public LeadAssignment updateAssignmentStatus(
            Long id,
            LeadAssignment.AssignmentStatus status) {

        if (status == null) {

            throw new IllegalArgumentException(
                    "Assignment status is required");
        }


        LeadAssignment assignment =
                leadAssignmentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Assignment not found"));


        // --------------------------------------------------------
        // WHEN MAKING ASSIGNMENT ACTIVE
        // CHECK LEAD STATUS
        // --------------------------------------------------------

        if (status ==
                LeadAssignment.AssignmentStatus.ACTIVE) {

            Lead lead =
                    leadRepository.findById(
                            assignment.getLeadId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Lead not found"));


            if (isClosedLead(lead)) {

                throw new IllegalArgumentException(
                        "Closed lead cannot have an active assignment. Reopen the lead first.");
            }


            Optional<LeadAssignment> existingActive =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    assignment.getLeadId(),
                                    LeadAssignment.AssignmentStatus.ACTIVE
                            );


            if (existingActive.isPresent()
                    &&
                    !existingActive.get()
                            .getId()
                            .equals(assignment.getId())) {

                throw new RuntimeException(
                        "Lead already has another active assignment");
            }
        }


        assignment.setStatus(status);

        return leadAssignmentRepository.save(
                assignment);
    }


    // ============================================================
    // DELETE ASSIGNMENT
    // ============================================================

    public void deleteAssignment(Long id) {

        if (!leadAssignmentRepository.existsById(id)) {

            throw new ResourceNotFoundException(
                    "Assignment not found");
        }

        leadAssignmentRepository.deleteById(id);
    }


    // ============================================================
    // CHECK WHETHER LEAD IS CLOSED
    // ============================================================

    private boolean isClosedLead(
            Lead lead) {

        if (lead == null ||
                lead.getStatus() == null) {

            return false;
        }


        return lead.getStatus() == Lead.LeadStatus.ENROLLED
                ||
                lead.getStatus()
                        == Lead.LeadStatus.NOT_INTERESTED
                ||
                lead.getStatus()
                        == Lead.LeadStatus.LOST
                ||
                lead.getStatus()
                        == Lead.LeadStatus.WRONG_NUMBER;
    }
}