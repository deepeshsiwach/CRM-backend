package ISFT.CRM.service;


import ISFT.CRM.entity.Note;
import ISFT.CRM.entity.User;
import ISFT.CRM.exception.ResourceNotFoundException;
import ISFT.CRM.repository.LeadRepository;
import ISFT.CRM.repository.NoteRepository;
import ISFT.CRM.repository.UserRepository;
import ISFT.CRM.repository.LeadAssignmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;

    public NoteService(
            NoteRepository noteRepository,
            LeadRepository leadRepository,
            UserRepository userRepository,
            LeadAssignmentRepository leadAssignmentRepository) {

        this.noteRepository = noteRepository;
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.leadAssignmentRepository = leadAssignmentRepository;
    }

    public List<Note> getAllNotes() {

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

        // Admin and Manager can see all notes
        if (!isAgent) {
            return noteRepository.findAll();
        }

        String email = authentication.getName();

        Long agentId = userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found"));

        // Get leads currently assigned to this agent
        List<Long> leadIds = leadAssignmentRepository
                .findByAgentIdAndStatus(
                        agentId,
                        ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                .stream()
                .map(ISFT.CRM.entity.LeadAssignment::getLeadId)
                .toList();

        if (leadIds.isEmpty()) {
            return List.of();
        }

        // Return notes only for the agent's assigned leads
        return noteRepository.findAll()
                .stream()
                .filter(note -> leadIds.contains(note.getLeadId()))
                .toList();
    }

    public Optional<Note> getNoteById(Long id) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            return Optional.empty();
        }

        Optional<Note> noteOptional =
                noteRepository.findById(id);

        if (noteOptional.isEmpty()) {
            return Optional.empty();
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));

        // Admin and Manager can access any note
        if (!isAgent) {
            return noteOptional;
        }

        String email = authentication.getName();

        Long agentId = userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found"));

        Note note = noteOptional.get();

        // Check whether the note's lead is currently assigned to this agent
        boolean assignedToAgent =
                leadAssignmentRepository
                        .findByLeadIdAndStatus(
                                note.getLeadId(),
                                ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                        .map(assignment ->
                                assignment.getAgentId().equals(agentId))
                        .orElse(false);

        if (!assignedToAgent) {
            return Optional.empty();
        }

        return noteOptional;
    }
    public Note updateNote(Long id, Note noteDetails) {

        Note existingNote = noteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Note not found"));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            throw new RuntimeException("Unauthorized access");
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));

        // Admin and Manager can update any note
        if (isAgent) {

            String email = authentication.getName();

            Long agentId = userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() ->
                            new RuntimeException("Authenticated user not found"));

            // Agent can update only notes belonging to their assigned lead
            boolean existingLeadAssigned =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    existingNote.getLeadId(),
                                    ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                            .map(assignment ->
                                    assignment.getAgentId().equals(agentId))
                            .orElse(false);

            if (!existingLeadAssigned) {

                throw new RuntimeException(
                        "You are not allowed to update this note");
            }

            // Check whether the NEW lead is also assigned to this agent
            boolean newLeadAssigned =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    noteDetails.getLeadId(),
                                    ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                            .map(assignment ->
                                    assignment.getAgentId().equals(agentId))
                            .orElse(false);

            if (!newLeadAssigned) {

                throw new RuntimeException(
                        "You are not allowed to move this note to this lead");
            }

            // Prevent agent from changing note ownership
            noteDetails.setUserId(agentId);
        }

        // Check whether the target lead exists
        if (!leadRepository.existsById(noteDetails.getLeadId())) {

            throw new ResourceNotFoundException("Lead not found");
        }

        // Check whether the target user exists
        User user = userRepository.findById(noteDetails.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getStatus() != User.Status.ACTIVE) {

            throw new RuntimeException("User is inactive");
        }

        existingNote.setLeadId(noteDetails.getLeadId());
        existingNote.setUserId(noteDetails.getUserId());
        existingNote.setNote(noteDetails.getNote());

        return noteRepository.save(existingNote);
    }
    public void deleteNote(Long id) {

        Note existingNote = noteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Note not found"));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            throw new RuntimeException("Unauthorized access");
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));

        // Admin and Manager can delete any note
        if (isAgent) {

            String email = authentication.getName();

            Long agentId = userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() ->
                            new RuntimeException("Authenticated user not found"));

            boolean assignedToAgent =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    existingNote.getLeadId(),
                                    ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                            .map(assignment ->
                                    assignment.getAgentId().equals(agentId))
                            .orElse(false);

            if (!assignedToAgent) {
                throw new RuntimeException(
                        "You are not allowed to delete this note");
            }
        }

        noteRepository.delete(existingNote);
    }


    public List<Note> getNotesByLead(Long leadId) {

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

        // Admin and Manager can view notes for any lead
        if (!isAgent) {
            return noteRepository.findByLeadId(leadId);
        }

        String email = authentication.getName();

        Long agentId = userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found"));

        // Check whether this lead is currently assigned to the agent
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

        return noteRepository.findByLeadId(leadId);
    }

    public List<Note> getNotesByUser(Long userId) {

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

        // Admin and Manager can view notes for any user
        if (!isAgent) {
            return noteRepository.findByUserId(userId);
        }

        String email = authentication.getName();

        Long agentId = userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found"));

        // Agent can only view their own notes
        if (!agentId.equals(userId)) {
            return List.of();
        }

        return noteRepository.findByUserId(agentId);
    }

    public Note createNote(Note note) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            throw new RuntimeException("Unauthorized access");
        }

        boolean isAgent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_AGENT"));

        // Check whether lead exists
        if (!leadRepository.existsById(note.getLeadId())) {
            throw new ResourceNotFoundException("Lead not found");
        }

        // Agent-specific security
        if (isAgent) {

            String email = authentication.getName();

            Long agentId = userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() ->
                            new RuntimeException("Authenticated user not found"));

            // Agent can create notes only for their assigned lead
            boolean assignedToAgent =
                    leadAssignmentRepository
                            .findByLeadIdAndStatus(
                                    note.getLeadId(),
                                    ISFT.CRM.entity.LeadAssignment.AssignmentStatus.ACTIVE)
                            .map(assignment ->
                                    assignment.getAgentId().equals(agentId))
                            .orElse(false);

            if (!assignedToAgent) {
                throw new RuntimeException(
                        "You are not allowed to create a note for this lead");
            }

            // Force the authenticated agent as the note owner
            note.setUserId(agentId);
        }

        // Validate the note user
        User user = userRepository.findById(note.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new RuntimeException("User is inactive");
        }

        return noteRepository.save(note);
    }
}