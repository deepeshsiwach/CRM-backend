package ISFT.CRM.service;

import ISFT.CRM.dto.AgentLeadDetailsUpdateRequest;
import ISFT.CRM.entity.Lead;
import ISFT.CRM.entity.LeadAssignment;
import ISFT.CRM.entity.User;
import ISFT.CRM.repository.LeadRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.Optional;

import ISFT.CRM.repository.LeadAssignmentRepository;
import ISFT.CRM.repository.CallLogRepository;
import ISFT.CRM.repository.FollowUpRepository;
import ISFT.CRM.repository.NoteRepository;
import ISFT.CRM.repository.UserRepository;
import ISFT.CRM.exception.ResourceNotFoundException;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final CallLogRepository callLogRepository;
    private final FollowUpRepository followUpRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public LeadService(
            LeadRepository leadRepository,
            LeadAssignmentRepository leadAssignmentRepository,
            CallLogRepository callLogRepository,
            FollowUpRepository followUpRepository,
            NoteRepository noteRepository,
            UserRepository userRepository) {

        this.leadRepository = leadRepository;
        this.leadAssignmentRepository = leadAssignmentRepository;
        this.callLogRepository = callLogRepository;
        this.followUpRepository = followUpRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }


    // ==========================================
    // GET ALL LEADS
    // ADMIN / MANAGER → ALL LEADS
    // AGENT → ONLY ACTIVE ASSIGNED LEADS
    // ==========================================

    public List<Lead> getAllLeads() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return Collections.emptyList();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        // ADMIN / MANAGER → all leads
        if (!isAgent) {
            return leadRepository.findAll();
        }

        // AGENT → only actively assigned leads
        String email = authentication.getName();

        Long agentId =
                userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Authenticated user not found"));

        List<Long> leadIds =
                leadAssignmentRepository
                        .findByAgentIdAndStatus(
                                agentId,
                                LeadAssignment.AssignmentStatus.ACTIVE)
                        .stream()
                        .map(LeadAssignment::getLeadId)
                        .collect(Collectors.toList());

        if (leadIds.isEmpty()) {
            return Collections.emptyList();
        }

        return leadRepository.findAllById(leadIds);
    }


    // ==========================================
    // GET LEADS BY CAMPAIGN
    // ==========================================

    public List<Lead> getLeadsByCampaign(Long campaignId) {

        return leadRepository.findByCampaignId(campaignId);
    }


    // ==========================================
    // GET LEADS FOR CURRENT USER
    // ==========================================

    public List<Lead> getLeadsForCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return Collections.emptyList();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        if (!isAgent) {
            return leadRepository.findAll();
        }

        String email = authentication.getName();

        Long agentId =
                userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Authenticated user not found"));

        List<Long> leadIds =
                leadAssignmentRepository
                        .findByAgentIdAndStatus(
                                agentId,
                                LeadAssignment.AssignmentStatus.ACTIVE)
                        .stream()
                        .map(LeadAssignment::getLeadId)
                        .collect(Collectors.toList());

        if (leadIds.isEmpty()) {
            return Collections.emptyList();
        }

        return leadRepository.findAllById(leadIds);
    }


    // ==========================================
    // GET LEAD BY ID
    // ==========================================

    public Optional<Lead> getLeadById(Long id) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return Optional.empty();
        }

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));

        Optional<Lead> leadOptional =
                leadRepository.findById(id);

        if (!isAgent) {
            return leadOptional;
        }

        if (leadOptional.isEmpty()) {
            return Optional.empty();
        }

        String email = authentication.getName();

        Long agentId =
                userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Authenticated user not found"));

        boolean assignedToAgent =
                leadAssignmentRepository
                        .findByLeadIdAndStatus(
                                id,
                                LeadAssignment.AssignmentStatus.ACTIVE)
                        .map(assignment ->
                                assignment.getAgentId()
                                        .equals(agentId))
                        .orElse(false);

        if (!assignedToAgent) {
            return Optional.empty();
        }

        return leadOptional;
    }


    // ==========================================
    // CREATE LEAD
    // ==========================================

    public Lead createLead(Lead lead) {

        if (lead.getFullName() == null ||
                lead.getFullName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Lead full name is required");
        }

        if (lead.getPhone() == null ||
                lead.getPhone().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Lead phone number is required");
        }

        if (!lead.getPhone().matches("\\+?[0-9]{10,15}")) {

            throw new IllegalArgumentException(
                    "Lead phone number must contain 10 to 15 digits");
        }

        // Check duplicate phone number

        if (leadRepository.existsByPhone(lead.getPhone())) {

            throw new IllegalArgumentException(
                    "A lead with this phone number already exists");
        }

        return leadRepository.save(lead);
    }


    // ==========================================
    // UPDATE LEAD
    // ==========================================

    public Lead updateLead(
            Long id,
            Lead leadDetails) {

        if (leadDetails.getFullName() == null ||
                leadDetails.getFullName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Lead full name is required");
        }

        if (leadDetails.getPhone() == null ||
                leadDetails.getPhone().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Lead phone number is required");
        }

        if (!leadDetails.getPhone().matches("\\+?[0-9]{10,15}")) {

            throw new IllegalArgumentException(
                    "Lead phone number must contain 10 to 15 digits");
        }

        // Check duplicate phone number during update

        if (leadRepository.existsByPhoneAndIdNot(
                leadDetails.getPhone(),
                id)) {

            throw new IllegalArgumentException(
                    "A lead with this phone number already exists");
        }

        Lead existingLead =
                leadRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Lead not found"));

        existingLead.setFullName(
                leadDetails.getFullName());

        existingLead.setEmail(
                leadDetails.getEmail());

        existingLead.setPhone(
                leadDetails.getPhone());

        existingLead.setCourseInterested(
                leadDetails.getCourseInterested());

        existingLead.setLeadSource(
                leadDetails.getLeadSource());

        existingLead.setStatus(
                leadDetails.getStatus());

        existingLead.setPriority(
                leadDetails.getPriority());

        existingLead.setCity(
                leadDetails.getCity());

        existingLead.setCampaignId(
                leadDetails.getCampaignId());

        return leadRepository.save(existingLead);
    }


    // ==========================================
    // UPDATE LEAD STATUS
    // ==========================================

    @Transactional
    public Lead updateLeadStatus(
            Long id,
            Lead.LeadStatus status) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Lead status is required");
        }

        Lead existingLead =
                leadRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Lead not found"));

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

        // ==========================================
        // AGENT CAN UPDATE ONLY HIS OWN LEADS
        // ==========================================

        if (isAgent) {

            String email =
                    authentication.getName();

            Long agentId =
                    userRepository.findByEmail(email)
                            .map(User::getId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Authenticated user not found"));

            boolean assignedToAgent =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    id,
                                    LeadAssignment.AssignmentStatus.ACTIVE)
                            .map(assignment ->
                                    assignment.getAgentId()
                                            .equals(agentId))
                            .orElse(false);

            if (!assignedToAgent) {

                throw new RuntimeException(
                        "You are not allowed to update the status of this lead");
            }
        }


        // ==========================================
        // UPDATE LEAD STATUS
        // ==========================================

        existingLead.setStatus(status);


        // ==========================================
        // FINAL STATUS
        // DEACTIVATE ACTIVE ASSIGNMENT
        // ==========================================

        if (status == Lead.LeadStatus.ENROLLED
                || status == Lead.LeadStatus.NOT_INTERESTED
                || status == Lead.LeadStatus.LOST
                || status == Lead.LeadStatus.WRONG_NUMBER) {

            leadAssignmentRepository
                    .findByLeadIdAndStatus(
                            id,
                            LeadAssignment.AssignmentStatus.ACTIVE
                    )
                    .ifPresent(assignment -> {

                        assignment.setStatus(
                                LeadAssignment.AssignmentStatus.INACTIVE
                        );

                        leadAssignmentRepository.save(
                                assignment
                        );

                    });
        }


        // ==========================================
        // SAVE LEAD
        // ==========================================

        return leadRepository.save(existingLead);
    }


    // ==========================================
    // UPDATE LEAD DETAILS BY AGENT
    // AGENT → ONLY HIS OWN ACTIVE LEADS
    // ADMIN / MANAGER → ANY LEAD
    // ==========================================

    @Transactional
    public Lead updateLeadDetailsByAgent(
            Long id,
            AgentLeadDetailsUpdateRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Lead details are required");
        }


        // ==========================================
        // FIND LEAD
        // ==========================================

        Lead existingLead =
                leadRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Lead not found"));


        // ==========================================
        // CURRENT AUTHENTICATED USER
        // ==========================================

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            throw new RuntimeException(
                    "Unauthorized access");
        }


        // ==========================================
        // CHECK IF CURRENT USER IS AGENT
        // ==========================================

        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));


        // ==========================================
        // AGENT → ONLY HIS ACTIVE ASSIGNED LEADS
        // ==========================================

        if (isAgent) {

            String email =
                    authentication.getName();

            Long agentId =
                    userRepository.findByEmail(email)
                            .map(User::getId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Authenticated user not found"));


            boolean assignedToAgent =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    id,
                                    LeadAssignment.AssignmentStatus.ACTIVE)
                            .map(assignment ->
                                    assignment.getAgentId()
                                            .equals(agentId))
                            .orElse(false);


            if (!assignedToAgent) {

                throw new RuntimeException(
                        "You are not allowed to update this lead");
            }
        }


        // ==========================================
        // VALIDATE FULL NAME
        // ==========================================

        if (request.getFullName() == null ||
                request.getFullName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Lead full name is required");
        }


        // ==========================================
        // VALIDATE PHONE
        // ==========================================

        if (request.getPhone() == null ||
                request.getPhone().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Lead phone number is required");
        }

        if (!request.getPhone()
                .matches("\\+?[0-9]{10,15}")) {

            throw new IllegalArgumentException(
                    "Lead phone number must contain 10 to 15 digits");
        }


        // ==========================================
        // CHECK DUPLICATE PHONE
        // ==========================================

        if (leadRepository.existsByPhoneAndIdNot(
                request.getPhone(),
                id)) {

            throw new IllegalArgumentException(
                    "A lead with this phone number already exists");
        }


        // ==========================================
        // VALIDATE AGE
        // ==========================================

        if (request.getAge() != null &&
                (request.getAge() < 1 ||
                        request.getAge() > 120)) {

            throw new IllegalArgumentException(
                    "Age must be between 1 and 120");
        }


        // ==========================================
        // UPDATE ALLOWED DETAILS
        // ==========================================

        existingLead.setFullName(
                request.getFullName().trim());

        existingLead.setEmail(
                cleanLeadDetail(request.getEmail()));

        existingLead.setPhone(
                request.getPhone().trim());

        existingLead.setAge(
                request.getAge());

        existingLead.setCity(
                cleanLeadDetail(request.getCity()));

        existingLead.setEducation(
                cleanLeadDetail(request.getEducation()));

        existingLead.setCurrentProfession(
                cleanLeadDetail(request.getCurrentProfession()));

        existingLead.setPrimaryObjective(
                cleanLeadDetail(request.getPrimaryObjective()));

        existingLead.setTradingInvestmentExperience(
                cleanLeadDetail(
                        request.getTradingInvestmentExperience()));

        existingLead.setCustomerLookingFor(
                cleanLeadDetail(
                        request.getCustomerLookingFor()));

        existingLead.setInterestedArea(
                cleanLeadDetail(
                        request.getInterestedArea()));


        // ==========================================
        // UPDATE PRIORITY
        // ==========================================

        if (request.getPriority() != null) {

            existingLead.setPriority(
                    request.getPriority());
        }


        // ==========================================
        // UPDATE STATUS
        // ==========================================

        if (request.getStatus() != null) {

            existingLead.setStatus(
                    request.getStatus());
        }


        // ==========================================
        // SAVE
        // ==========================================

        return leadRepository.save(existingLead);
    }


    // ==========================================
    // CLEAN LEAD DETAIL
    // ==========================================

    private String cleanLeadDetail(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }


    // ==========================================
    // DELETE LEAD
    // ==========================================

    @Transactional
    public void deleteLead(Long id) {

        if (!leadRepository.existsById(id)) {

            throw new ResourceNotFoundException(
                    "Lead not found");
        }

        // Delete related records first

        leadAssignmentRepository.deleteByLeadId(id);

        callLogRepository.deleteByLeadId(id);

        followUpRepository.deleteByLeadId(id);

        noteRepository.deleteByLeadId(id);

        // Delete the lead

        leadRepository.deleteById(id);
    }
}