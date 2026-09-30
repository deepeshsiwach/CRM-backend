package ISFT.CRM.service;


import ISFT.CRM.entity.Team;
import ISFT.CRM.repository.TeamRepository;
import ISFT.CRM.repository.LeadAssignmentRepository;
import ISFT.CRM.entity.LeadAssignment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;

    public TeamService(
            TeamRepository teamRepository,
            LeadAssignmentRepository leadAssignmentRepository) {

        this.teamRepository = teamRepository;
        this.leadAssignmentRepository =
                leadAssignmentRepository;
    }


    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Optional<Team> getTeamById(Long id) {
        return teamRepository.findById(id);
    }

    public Team createTeam(Team team) {
        if (team.getTeamName() == null ||
                team.getTeamName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Team name is required");
        }
        return teamRepository.save(team);
    }

    public Team updateTeam(Long id, Team teamDetails) {
        if (teamDetails.getTeamName() == null ||
                teamDetails.getTeamName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Team name is required");
        }

        Team existingTeam = teamRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Team not found"));

        existingTeam.setTeamName(teamDetails.getTeamName());
        existingTeam.setDescription(teamDetails.getDescription());
        existingTeam.setStatus(teamDetails.getStatus());

        return teamRepository.save(existingTeam);
    }

    public void deleteTeam(Long id) {

        if (!teamRepository.existsById(id)) {
            throw new RuntimeException("Team not found");
        }

        List<LeadAssignment> assignments =
                leadAssignmentRepository.findByTeamId(id);

        if (!assignments.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete team because it is used by lead assignments."
            );
        }

        teamRepository.deleteById(id);
    }
}