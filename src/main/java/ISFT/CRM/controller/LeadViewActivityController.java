
package ISFT.CRM.controller;

import ISFT.CRM.entity.LeadAssignment;
import ISFT.CRM.entity.User;
import ISFT.CRM.repository.LeadAssignmentRepository;
import ISFT.CRM.repository.UserRepository;
import ISFT.CRM.service.LeadViewActivityService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/lead-views")
public class LeadViewActivityController {

    private final LeadViewActivityService leadViewActivityService;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final UserRepository userRepository;

    public LeadViewActivityController(
            LeadViewActivityService leadViewActivityService,
            LeadAssignmentRepository leadAssignmentRepository,
            UserRepository userRepository) {
        this.leadViewActivityService = leadViewActivityService;
        this.leadAssignmentRepository = leadAssignmentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{leadId}")
    public Map<String, Object> recordLeadView(@PathVariable Long leadId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        boolean isAgent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));

        if (!isAgent) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only agents can record lead views");
        }

        User agent = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        boolean assignedToAgent = leadAssignmentRepository
                .findByAgentIdAndStatus(
                        agent.getId(),
                        LeadAssignment.AssignmentStatus.ACTIVE)
                .stream()
                .anyMatch(assignment ->
                        leadId.equals(assignment.getLeadId()));

        if (!assignedToAgent) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This lead is not actively assigned to you");
        }

        leadViewActivityService.recordView(agent.getId(), leadId);

        return Map.of(
                "success", true,
                "message", "Lead view recorded",
                "leadId", leadId
        );
    }
}
