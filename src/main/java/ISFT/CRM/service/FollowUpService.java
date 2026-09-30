package ISFT.CRM.service;

import ISFT.CRM.entity.FollowUp;
import ISFT.CRM.entity.User;
import ISFT.CRM.entity.Lead;
import ISFT.CRM.entity.LeadAssignment;
import ISFT.CRM.exception.ResourceNotFoundException;
import ISFT.CRM.repository.FollowUpRepository;
import ISFT.CRM.repository.LeadRepository;
import ISFT.CRM.repository.UserRepository;
import ISFT.CRM.repository.LeadAssignmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final LeadService leadService;

    public FollowUpService(
            FollowUpRepository followUpRepository,
            LeadRepository leadRepository,
            UserRepository userRepository,
            LeadAssignmentRepository leadAssignmentRepository,
            LeadService leadService) {

        this.followUpRepository = followUpRepository;
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.leadAssignmentRepository = leadAssignmentRepository;
        this.leadService = leadService;
    }


    // ========================================
    // GET ALL FOLLOW-UPS
    // ADMIN / MANAGER -> ALL FOLLOW-UPS
    // AGENT -> ONLY FOLLOW-UPS FOR ACTIVE
    // ASSIGNED LEADS
    // ========================================

    public List<FollowUp> getAllFollowUps() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        if (!isAgent) {
            return followUpRepository.findAll();
        }

        Long agentId =
                getAuthenticatedAgentId();

        Set<Long> activeLeadIds =
                getActiveLeadIdsForAgent(agentId);

        return followUpRepository
                .findByAgentId(agentId)
                .stream()
                .filter(followUp ->
                        activeLeadIds.contains(
                                followUp.getLeadId()))
                .toList();
    }


    // ========================================
    // GET FOLLOW-UPS BY LEAD
    // ========================================

    public List<FollowUp> getFollowUpsByLead(
            Long leadId) {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        // Can view follow-ups for any lead
        if (!isAgent) {
            return followUpRepository.findByLeadId(leadId);
        }

        Long agentId =
                getAuthenticatedAgentId();

        // AGENT must currently be assigned to the lead
        if (!isLeadAssignedToAgent(
                leadId,
                agentId)) {

            return List.of();
        }

        // Return ALL follow-up history for this lead,
        // regardless of which agent created the follow-up.
        return followUpRepository.findByLeadId(leadId);
    }


    // ========================================
    // GET FOLLOW-UPS BY AGENT
    // ========================================

    public List<FollowUp> getFollowUpsByAgent(
            Long agentId) {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        if (!isAgent) {
            return followUpRepository
                    .findByAgentId(agentId);
        }

        Long authenticatedAgentId =
                getAuthenticatedAgentId();

        // AGENT can only access own follow-ups
        if (!authenticatedAgentId.equals(agentId)) {
            return List.of();
        }

        Set<Long> activeLeadIds =
                getActiveLeadIdsForAgent(
                        authenticatedAgentId);

        return followUpRepository
                .findByAgentId(authenticatedAgentId)
                .stream()
                .filter(followUp ->
                        activeLeadIds.contains(
                                followUp.getLeadId()))
                .toList();
    }


    // ========================================
    // GET FOLLOW-UPS BY STATUS
    // ========================================

    public List<FollowUp> getFollowUpsByStatus(
            FollowUp.FollowUpStatus status) {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        if (!isAgent) {
            return followUpRepository
                    .findByStatus(status);
        }

        Long agentId =
                getAuthenticatedAgentId();

        Set<Long> activeLeadIds =
                getActiveLeadIdsForAgent(agentId);

        return followUpRepository
                .findByStatus(status)
                .stream()
                .filter(followUp ->
                        Objects.equals(
                                followUp.getAgentId(),
                                agentId))
                .filter(followUp ->
                        activeLeadIds.contains(
                                followUp.getLeadId()))
                .toList();
    }


    // ========================================
    // GET FOLLOW-UPS BY AGENT + STATUS
    // ========================================

    public List<FollowUp> getFollowUpsByAgentAndStatus(
            Long agentId,
            FollowUp.FollowUpStatus status) {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        if (!isAgent) {
            return followUpRepository
                    .findByAgentIdAndStatus(
                            agentId,
                            status);
        }

        Long authenticatedAgentId =
                getAuthenticatedAgentId();

        // AGENT can only query own follow-ups
        if (!authenticatedAgentId.equals(agentId)) {
            return List.of();
        }

        Set<Long> activeLeadIds =
                getActiveLeadIdsForAgent(
                        authenticatedAgentId);

        return followUpRepository
                .findByAgentIdAndStatus(
                        authenticatedAgentId,
                        status)
                .stream()
                .filter(followUp ->
                        activeLeadIds.contains(
                                followUp.getLeadId()))
                .toList();
    }


    // ========================================
    // GET FOLLOW-UP BY ID
    // ========================================

    public Optional<FollowUp> getFollowUpById(
            Long id) {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return Optional.empty();
        }

        Optional<FollowUp> followUpOptional =
                followUpRepository.findById(id);

        if (followUpOptional.isEmpty()) {
            return Optional.empty();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        if (!isAgent) {
            return followUpOptional;
        }

        Long agentId =
                getAuthenticatedAgentId();

        FollowUp followUp =
                followUpOptional.get();

        // Must belong to authenticated agent
        if (!Objects.equals(
                followUp.getAgentId(),
                agentId)) {

            return Optional.empty();
        }

        // Lead must still be actively assigned
        if (!isLeadAssignedToAgent(
                followUp.getLeadId(),
                agentId)) {

            return Optional.empty();
        }

        return followUpOptional;
    }


    // ========================================
    // OVERDUE FOLLOW-UPS
    // ========================================

    public List<FollowUp> getOverdueFollowUps() {

        LocalDateTime now =
                LocalDateTime.now();

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        if (!isAgent) {

            return followUpRepository
                    .findByFollowUpDateBeforeAndStatus(
                            now,
                            FollowUp.FollowUpStatus.PENDING);
        }

        Long agentId =
                getAuthenticatedAgentId();

        Set<Long> activeLeadIds =
                getActiveLeadIdsForAgent(agentId);

        return followUpRepository
                .findByAgentIdAndFollowUpDateBeforeAndStatus(
                        agentId,
                        now,
                        FollowUp.FollowUpStatus.PENDING)
                .stream()
                .filter(followUp ->
                        activeLeadIds.contains(
                                followUp.getLeadId()))
                .toList();
    }


    // ========================================
    // TODAY'S FOLLOW-UPS
    // ========================================

    public List<FollowUp> getTodayFollowUps() {

        LocalDate today =
                LocalDate.now();

        LocalDateTime start =
                today.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1)
                        .atStartOfDay();

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        if (!isAgent) {

            return followUpRepository
                    .findByFollowUpDateBetweenAndStatus(
                            start,
                            end,
                            FollowUp.FollowUpStatus.PENDING);
        }

        Long agentId =
                getAuthenticatedAgentId();

        Set<Long> activeLeadIds =
                getActiveLeadIdsForAgent(agentId);

        return followUpRepository
                .findByAgentIdAndFollowUpDateBetweenAndStatus(
                        agentId,
                        start,
                        end,
                        FollowUp.FollowUpStatus.PENDING)
                .stream()
                .filter(followUp ->
                        activeLeadIds.contains(
                                followUp.getLeadId()))
                .toList();
    }


    // ========================================
    // UPCOMING FOLLOW-UPS
    // ========================================

    public List<FollowUp> getUpcomingFollowUps() {

        LocalDateTime start =
                LocalDateTime.now();

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return List.of();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER
        if (!isAgent) {

            return followUpRepository
                    .findByFollowUpDateAfterAndStatus(
                            start,
                            FollowUp.FollowUpStatus.PENDING);
        }

        Long agentId =
                getAuthenticatedAgentId();

        Set<Long> activeLeadIds =
                getActiveLeadIdsForAgent(agentId);

        return followUpRepository
                .findByAgentIdAndFollowUpDateAfterAndStatus(
                        agentId,
                        start,
                        FollowUp.FollowUpStatus.PENDING)
                .stream()
                .filter(followUp ->
                        activeLeadIds.contains(
                                followUp.getLeadId()))
                .toList();
    }


    // ========================================
    // GET AUTHENTICATED AGENT ID
    // ========================================

    private Long getAuthenticatedAgentId() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null) {

            throw new RuntimeException(
                    "Unauthorized access");
        }

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"));
    }


    // ========================================
    // GET ACTIVE LEAD IDS FOR AGENT
    // ========================================

    private Set<Long> getActiveLeadIdsForAgent(
            Long agentId) {

        return leadAssignmentRepository
                .findByAgentIdAndStatus(
                        agentId,
                        LeadAssignment.AssignmentStatus.ACTIVE)
                .stream()
                .map(LeadAssignment::getLeadId)
                .collect(Collectors.toSet());
    }


    // ========================================
    // CHECK ACTIVE LEAD ASSIGNMENT
    // ========================================

    private boolean isLeadAssignedToAgent(
            Long leadId,
            Long agentId) {

        return leadAssignmentRepository
                .findByLeadIdAndStatus(
                        leadId,
                        LeadAssignment.AssignmentStatus.ACTIVE)
                .map(assignment ->
                        Objects.equals(
                                assignment.getAgentId(),
                                agentId))
                .orElse(false);
    }


    // ========================================
    // UPDATE FOLLOW-UP
    // ========================================

    public FollowUp updateFollowUp(
            Long id,
            FollowUp followUpDetails) {

        FollowUp existingFollowUp =
                followUpRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Follow-up not found"));

        Authentication authentication =
                SecurityContextHolder.getContext()
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

        // ========================================
        // AGENT SECURITY
        // ========================================

        if (isAgent) {

            Long agentId =
                    getAuthenticatedAgentId();

            // Existing follow-up must belong to agent
            if (!Objects.equals(
                    existingFollowUp.getAgentId(),
                    agentId)) {

                throw new RuntimeException(
                        "You are not allowed to update this follow-up");
            }

            // Existing lead must still be assigned
            if (!isLeadAssignedToAgent(
                    existingFollowUp.getLeadId(),
                    agentId)) {

                throw new RuntimeException(
                        "You are not allowed to update this follow-up because the lead is not actively assigned to you");
            }

            // New lead must be assigned to same agent
            if (!isLeadAssignedToAgent(
                    followUpDetails.getLeadId(),
                    agentId)) {

                throw new RuntimeException(
                        "You are not allowed to move this follow-up to this lead");
            }

            // Agent cannot change ownership
            followUpDetails.setAgentId(agentId);
        }


        // ========================================
        // TARGET LEAD MUST EXIST
        // ========================================

        if (!leadRepository.existsById(
                followUpDetails.getLeadId())) {

            throw new ResourceNotFoundException(
                    "Lead not found");
        }


        // ========================================
        // UPDATE FIELDS
        // ========================================

        existingFollowUp.setLeadId(
                followUpDetails.getLeadId());

        existingFollowUp.setAgentId(
                followUpDetails.getAgentId());

        existingFollowUp.setFollowUpDate(
                followUpDetails.getFollowUpDate());

        existingFollowUp.setPurpose(
                followUpDetails.getPurpose());

        existingFollowUp.setStatus(
                followUpDetails.getStatus());

        existingFollowUp.setRemarks(
                followUpDetails.getRemarks());


        return followUpRepository.save(
                existingFollowUp);
    }


    // ========================================
    // DELETE FOLLOW-UP
    // ========================================

    public void deleteFollowUp(Long id) {

        FollowUp existingFollowUp =
                followUpRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Follow-up not found"));

        Authentication authentication =
                SecurityContextHolder.getContext()
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

        // ========================================
        // AGENT SECURITY
        // ========================================

        if (isAgent) {

            Long agentId =
                    getAuthenticatedAgentId();

            if (!Objects.equals(
                    existingFollowUp.getAgentId(),
                    agentId)) {

                throw new RuntimeException(
                        "You are not allowed to delete this follow-up");
            }

            if (!isLeadAssignedToAgent(
                    existingFollowUp.getLeadId(),
                    agentId)) {

                throw new RuntimeException(
                        "You are not allowed to delete this follow-up because the lead is not actively assigned to you");
            }
        }


        followUpRepository.delete(
                existingFollowUp);
    }


    // ========================================
    // UPDATE FOLLOW-UP STATUS
    // ========================================

    public FollowUp updateFollowUpStatus(
            Long id,
            FollowUp.FollowUpStatus status) {

        if (status == null) {

            throw new IllegalArgumentException(
                    "Follow-up status is required");
        }

        FollowUp followUp =
                followUpRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Follow-up not found"));

        Authentication authentication =
                SecurityContextHolder.getContext()
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

        // ========================================
        // AGENT SECURITY
        // ========================================

        if (isAgent) {

            Long agentId =
                    getAuthenticatedAgentId();

            if (!Objects.equals(
                    followUp.getAgentId(),
                    agentId)) {

                throw new RuntimeException(
                        "You are not allowed to update this follow-up");
            }

            if (!isLeadAssignedToAgent(
                    followUp.getLeadId(),
                    agentId)) {

                throw new RuntimeException(
                        "You are not allowed to update this follow-up because the lead is not actively assigned to you");
            }
        }


        followUp.setStatus(status);

        return followUpRepository.save(
                followUp);
    }


    // ========================================
    // CREATE FOLLOW-UP
    // ========================================

    public FollowUp createFollowUp(
            FollowUp followUp) {

        Authentication authentication =
                SecurityContextHolder.getContext()
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


        // ========================================
        // CHECK LEAD
        // ========================================

        if (!leadRepository.existsById(
                followUp.getLeadId())) {

            throw new ResourceNotFoundException(
                    "Lead not found");
        }


        // ========================================
        // AGENT SECURITY
        // ========================================

        if (isAgent) {

            Long agentId =
                    getAuthenticatedAgentId();

            // Lead must be actively assigned
            if (!isLeadAssignedToAgent(
                    followUp.getLeadId(),
                    agentId)) {

                throw new RuntimeException(
                        "You are not allowed to create a follow-up for this lead");
            }

            // Always use authenticated agent
            followUp.setAgentId(agentId);
        }


        // ========================================
        // CHECK AGENT
        // ========================================

        User agent =
                userRepository
                        .findById(followUp.getAgentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agent not found"));


        // Selected user must be AGENT
        if (agent.getRole() != User.Role.AGENT) {

            throw new RuntimeException(
                    "Selected user is not an agent");
        }


        // Agent must be active
        if (agent.getStatus() != User.Status.ACTIVE) {

            throw new RuntimeException(
                    "Agent is inactive");
        }


        // ========================================
        // SAVE FOLLOW-UP
        // ========================================

        FollowUp savedFollowUp =
                followUpRepository.save(followUp);


        // ========================================
        // AUTOMATIC LEAD STATUS UPDATE
        // PENDING -> FOLLOW_UP
        // ========================================

        if (followUp.getStatus() ==
                FollowUp.FollowUpStatus.PENDING) {

            leadService.updateLeadStatus(
                    followUp.getLeadId(),
                    Lead.LeadStatus.FOLLOW_UP
            );
        }


        return savedFollowUp;
    }
}