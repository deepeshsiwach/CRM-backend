 package ISFT.CRM.controller;

import ISFT.CRM.entity.Team;
import ISFT.CRM.entity.LeadAssignment;
import ISFT.CRM.repository.LeadAssignmentRepository;
import ISFT.CRM.repository.UserRepository;
import ISFT.CRM.service.TeamService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final UserRepository userRepository;

    public TeamController(
            TeamService teamService,
            LeadAssignmentRepository leadAssignmentRepository,
            UserRepository userRepository) {

        this.teamService = teamService;
        this.leadAssignmentRepository = leadAssignmentRepository;
        this.userRepository = userRepository;
    }

    // Get all teams
    @GetMapping
    public List<Team> getAllTeams() {
        return teamService.getAllTeams();
    }

    // Get a team by ID
    @GetMapping("/{id}")
    public Team getTeamById(@PathVariable Long id) {
        return teamService.getTeamById(id)
                .orElseThrow(() ->
                        new RuntimeException("Team not found"));
    }

    // Get members and their active lead counts
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getTeamMembers(
            @PathVariable Long id) {

        // Verify that the team exists
        teamService.getTeamById(id)
                .orElseThrow(() ->
                        new RuntimeException("Team not found"));

        // Get assignments belonging to this team
        List<LeadAssignment> assignments =
                leadAssignmentRepository.findByTeamId(id);

        // Group assignments by agent ID
        Map<Long, List<LeadAssignment>> assignmentsByAgent =
                assignments.stream()
                        .collect(Collectors.groupingBy(
                                LeadAssignment::getAgentId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<Map<String, Object>> members =
                new ArrayList<>();

        for (Map.Entry<Long, List<LeadAssignment>> entry
                : assignmentsByAgent.entrySet()) {

            Long agentId = entry.getKey();

            userRepository.findById(agentId)
                    .ifPresent(user -> {

                        long activeLeadCount =
                                entry.getValue()
                                        .stream()
                                        .filter(assignment ->
                                                assignment.getStatus()
                                                        == LeadAssignment
                                                        .AssignmentStatus
                                                        .ACTIVE)
                                        .map(LeadAssignment::getLeadId)
                                        .distinct()
                                        .count();

                        Map<String, Object> member =
                                new LinkedHashMap<>();

                        member.put("id", user.getId());
                        member.put("fullName",
                                user.getFullName());
                        member.put("email",
                                user.getEmail());
                        member.put("phone",
                                user.getPhone());
                        member.put("role",
                                user.getRole());
                        member.put("status",
                                user.getStatus());
                        member.put("activeLeadCount",
                                activeLeadCount);

                        members.add(member);
                    });
        }

        return ResponseEntity.ok(members);
    }

    // Create a team
    @PostMapping
    public Team createTeam(@RequestBody Team team) {
        return teamService.createTeam(team);
    }

    // Update a team
    @PutMapping("/{id}")
    public Team updateTeam(
            @PathVariable Long id,
            @RequestBody Team teamDetails) {

        return teamService.updateTeam(id, teamDetails);
    }

    // Delete a team
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTeam(
            @PathVariable Long id) {

        try {
            teamService.deleteTeam(id);
            return ResponseEntity.noContent().build();

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(409)
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }
}