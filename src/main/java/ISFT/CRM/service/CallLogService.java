package ISFT.CRM.service;

import ISFT.CRM.entity.CallLog;
import ISFT.CRM.entity.Lead;
import ISFT.CRM.entity.User;
import ISFT.CRM.exception.ResourceNotFoundException;
import ISFT.CRM.repository.CallLogRepository;
import ISFT.CRM.repository.LeadRepository;
import ISFT.CRM.repository.UserRepository;
import ISFT.CRM.repository.LeadAssignmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;

@Service
public class CallLogService {

    private final CallLogRepository callLogRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final LeadService leadService;


    public CallLogService(
            CallLogRepository callLogRepository,
            LeadRepository leadRepository,
            UserRepository userRepository,
            LeadAssignmentRepository leadAssignmentRepository,
            @Lazy LeadService leadService) {

        this.callLogRepository = callLogRepository;
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.leadAssignmentRepository = leadAssignmentRepository;
        this.leadService = leadService;
    }


    // ========================================
    // GET ALL CALL LOGS
    // ========================================

    public List<CallLog> getAllCallLogs() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));

        if (!isAgent) {
            return callLogRepository.findAll();
        }

        String email = authentication.getName();

        Long agentId = userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"));

        return callLogRepository.findByAgentId(agentId);
    }


    // ========================================
    // GET CALL LOGS BY LEAD
    // ========================================

    public List<CallLog> getCallLogsByLead(Long leadId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));

        // Admin and Manager can view calls for any lead
        if (!isAgent) {

            return callLogRepository.findByLeadId(leadId);
        }


        String email = authentication.getName();

        Long agentId = userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"));


        // Agent can only view calls for their active assigned lead
        boolean assignedToAgent =
                leadAssignmentRepository
                        .findByLeadIdAndStatus(
                                leadId,
                                ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                        .map(assignment ->
                                assignment.getAgentId().equals(agentId))
                        .orElse(false);


        if (!assignedToAgent) {

            return List.of();
        }


        return callLogRepository.findByLeadId(leadId);
    }


    // ========================================
    // GET CALL LOGS BY LEAD IDS
    // ========================================

    public List<CallLog> getCallLogsByLeadIds(
            List<Long> leadIds) {

        if (leadIds == null ||
                leadIds.isEmpty()) {

            return List.of();
        }

        return callLogRepository.findByLeadIdIn(leadIds);
    }


    // ========================================
    // GET CALL LOGS BY CAMPAIGN
    // ========================================

    public List<CallLog> getCallLogsByCampaign(
            Long campaignId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));


        List<Long> leadIds = leadRepository
                .findByCampaignId(campaignId)
                .stream()
                .map(lead -> lead.getId())
                .toList();


        if (leadIds.isEmpty()) {

            return List.of();
        }


        // Admin and Manager can view all campaign calls
        if (!isAgent) {

            return callLogRepository
                    .findByLeadIdIn(leadIds);
        }


        String email = authentication.getName();

        Long agentId = userRepository
                .findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"));


        // Agent can only view calls for assigned leads
        List<Long> assignedLeadIds =
                leadIds.stream()
                        .filter(leadId ->
                                leadAssignmentRepository
                                        .findByLeadIdAndStatus(
                                                leadId,
                                                ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                                        .map(assignment ->
                                                assignment.getAgentId()
                                                        .equals(agentId))
                                        .orElse(false))
                        .toList();


        if (assignedLeadIds.isEmpty()) {

            return List.of();
        }


        return callLogRepository
                .findByLeadIdIn(assignedLeadIds);
    }


    // ========================================
    // GET CALL LOGS BY AGENT
    // ========================================

    public List<CallLog> getCallLogsByAgent(
            Long agentId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));


        // Admin and Manager can view any agent
        if (!isAgent) {

            return callLogRepository
                    .findByAgentId(agentId);
        }


        String email = authentication.getName();

        Long authenticatedAgentId =
                userRepository
                        .findByEmail(email)
                        .map(User::getId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Authenticated user not found"));


        // Agent can only view their own calls
        if (!authenticatedAgentId.equals(agentId)) {

            return List.of();
        }


        return callLogRepository
                .findByAgentId(authenticatedAgentId);
    }


    // ========================================
    // GET CALL LOGS BY STATUS
    // ========================================

    public List<CallLog> getCallLogsByStatus(
            CallLog.CallStatus callStatus) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));


        // Admin and Manager
        if (!isAgent) {

            return callLogRepository
                    .findByCallStatus(callStatus);
        }


        String email = authentication.getName();

        Long agentId = userRepository
                .findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"));


        return callLogRepository
                .findByCallStatus(callStatus)
                .stream()
                .filter(callLog ->
                        callLog.getAgentId()
                                .equals(agentId))
                .toList();
    }


    // ========================================
    // GET CALL LOGS BY OUTCOME
    // ========================================

    public List<CallLog> getCallLogsByOutcome(
            CallLog.CallOutcome callOutcome) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));


        // Admin and Manager
        if (!isAgent) {

            return callLogRepository
                    .findByCallOutcome(callOutcome);
        }


        String email = authentication.getName();

        Long agentId = userRepository
                .findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"));


        return callLogRepository
                .findByCallOutcome(callOutcome)
                .stream()
                .filter(callLog ->
                        callLog.getAgentId()
                                .equals(agentId))
                .toList();
    }


    // ========================================
    // GET CALL LOG BY ID
    // ========================================

    public Optional<CallLog> getCallLogById(
            Long id) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return Optional.empty();
        }


        Optional<CallLog> callLogOptional =
                callLogRepository.findById(id);


        if (callLogOptional.isEmpty()) {

            return Optional.empty();
        }


        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));


        if (!isAgent) {

            return callLogOptional;
        }


        String email = authentication.getName();

        Long agentId = userRepository
                .findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"));


        CallLog callLog =
                callLogOptional.get();


        if (!callLog.getAgentId()
                .equals(agentId)) {

            return Optional.empty();
        }


        return callLogOptional;
    }


    // ========================================
    // UPDATE CALL LOG
    // ========================================

    @Transactional
    public CallLog updateCallLog(
            Long id,
            CallLog callLogDetails) {

        CallLog existingCallLog =
                callLogRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Call log not found"));


        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            throw new RuntimeException(
                    "Unauthorized access");
        }


        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));


        // Admin and Manager can update any call log
        if (isAgent) {

            String email =
                    authentication.getName();


            Long agentId =
                    userRepository
                            .findByEmail(email)
                            .map(User::getId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Authenticated user not found"));


            // Agent can update only their own call logs
            if (!existingCallLog.getAgentId()
                    .equals(agentId)) {

                throw new RuntimeException(
                        "You are not allowed to update this call log");
            }


            // Prevent agent from changing ownership
            callLogDetails.setAgentId(agentId);


            // Check new lead assignment
            boolean assignedToNewLead =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    callLogDetails.getLeadId(),
                                    ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                            .map(assignment ->
                                    assignment.getAgentId()
                                            .equals(agentId))
                            .orElse(false);


            if (!assignedToNewLead) {

                throw new RuntimeException(
                        "You are not allowed to move this call log to this lead");
            }
        }


        // Check target lead
        if (!leadRepository.existsById(
                callLogDetails.getLeadId())) {

            throw new ResourceNotFoundException(
                    "Lead not found");
        }


        existingCallLog.setLeadId(
                callLogDetails.getLeadId());

        existingCallLog.setAgentId(
                callLogDetails.getAgentId());

        existingCallLog.setCallStartTime(
                callLogDetails.getCallStartTime());

        existingCallLog.setCallEndTime(
                callLogDetails.getCallEndTime());

        existingCallLog.setDurationSeconds(
                callLogDetails.getDurationSeconds());

        existingCallLog.setCallStatus(
                callLogDetails.getCallStatus());

        existingCallLog.setCallOutcome(
                callLogDetails.getCallOutcome());

        existingCallLog.setRemarks(
                callLogDetails.getRemarks());


        CallLog savedCallLog =
                callLogRepository.save(
                        existingCallLog);


        // ========================================
        // AUTOMATIC LEAD STATUS UPDATE
        // ========================================

        updateLeadStatusFromCallOutcome(
                savedCallLog);


        return savedCallLog;
    }


    // ========================================
    // CREATE CALL LOG
    // ========================================

    @Transactional
    public CallLog createCallLog(
            CallLog callLog) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            throw new RuntimeException(
                    "Unauthorized access");
        }


        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));


        // Check whether lead exists
        if (!leadRepository.existsById(
                callLog.getLeadId())) {

            throw new ResourceNotFoundException(
                    "Lead not found");
        }


        if (isAgent) {

            String email =
                    authentication.getName();


            Long agentId =
                    userRepository
                            .findByEmail(email)
                            .map(User::getId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Authenticated user not found"));


            // Agent can create calls only for assigned leads
            boolean assignedToAgent =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    callLog.getLeadId(),
                                    ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                            .map(assignment ->
                                    assignment.getAgentId()
                                            .equals(agentId))
                            .orElse(false);


            if (!assignedToAgent) {

                throw new RuntimeException(
                        "You are not allowed to create a call log for this lead");
            }


            // Always use authenticated agent
            callLog.setAgentId(agentId);
        }


        // Check agent exists
        User agent =
                userRepository
                        .findById(callLog.getAgentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agent not found"));


        // Check selected user is an agent
        if (agent.getRole() !=
                User.Role.AGENT) {

            throw new RuntimeException(
                    "Selected user is not an agent");
        }


        // Check agent is active
        if (agent.getStatus() !=
                User.Status.ACTIVE) {

            throw new RuntimeException(
                    "Agent is inactive");
        }


        CallLog savedCallLog =
                callLogRepository.save(
                        callLog);


        // ========================================
        // AUTOMATIC LEAD STATUS UPDATE
        // ========================================

        updateLeadStatusFromCallOutcome(
                savedCallLog);


        return savedCallLog;
    }


    // ========================================
    // CALL OUTCOME → LEAD STATUS
    // ========================================

    private void updateLeadStatusFromCallOutcome(
            CallLog callLog) {

        if (callLog.getCallOutcome() == null) {

            return;
        }


        Lead.LeadStatus newStatus = null;


        switch (callLog.getCallOutcome()) {

            case INTERESTED:

                newStatus =
                        Lead.LeadStatus.INTERESTED;

                break;


            case FOLLOW_UP:

                newStatus =
                        Lead.LeadStatus.FOLLOW_UP;

                break;


            case COUNSELLING:

                newStatus =
                        Lead.LeadStatus.COUNSELLING;

                break;


            case ENROLLED:

                newStatus =
                        Lead.LeadStatus.ENROLLED;

                break;


            case NOT_INTERESTED:

                newStatus =
                        Lead.LeadStatus.NOT_INTERESTED;

                break;


            case WRONG_NUMBER:

                newStatus =
                        Lead.LeadStatus.WRONG_NUMBER;

                break;


            case NO_RESPONSE:

                newStatus =
                        Lead.LeadStatus.NO_RESPONSE;

                break;


            case CALL_BACK:

                newStatus =
                        Lead.LeadStatus.FOLLOW_UP;

                break;


            default:

                break;
        }


        if (newStatus != null) {

            leadService.updateLeadStatus(
                    callLog.getLeadId(),
                    newStatus
            );
        }
    }


    // ========================================
    // DELETE CALL LOG
    // ========================================

    public void deleteCallLog(Long id) {

        CallLog existingCallLog =
                callLogRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Call log not found"));


        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            throw new RuntimeException(
                    "Unauthorized access");
        }


        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));


        // Admin and Manager can delete any call log
        if (isAgent) {

            String email =
                    authentication.getName();


            Long agentId =
                    userRepository
                            .findByEmail(email)
                            .map(User::getId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Authenticated user not found"));


            // Agent can delete only their own call logs
            if (!existingCallLog.getAgentId()
                    .equals(agentId)) {

                throw new RuntimeException(
                        "You are not allowed to delete this call log");
            }
        }


        callLogRepository.delete(
                existingCallLog);
    }
}