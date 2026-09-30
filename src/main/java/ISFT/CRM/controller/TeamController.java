package ISFT.CRM.controller;

import ISFT.CRM.entity.Team;
import ISFT.CRM.service.TeamService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<Team> getAllTeams() {
        return teamService.getAllTeams();
    }

    @GetMapping("/{id}")
    public Team getTeamById(@PathVariable Long id) {
        return teamService.getTeamById(id)
                .orElseThrow(() ->
                        new RuntimeException("Team not found"));
    }

    @PostMapping
    public Team createTeam(@RequestBody Team team) {
        return teamService.createTeam(team);
    }

    @PutMapping("/{id}")
    public Team updateTeam(
            @PathVariable Long id,
            @RequestBody Team teamDetails) {

        return teamService.updateTeam(id, teamDetails);
    }

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