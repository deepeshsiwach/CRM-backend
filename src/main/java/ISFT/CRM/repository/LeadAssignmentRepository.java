package ISFT.CRM.repository;

import ISFT.CRM.entity.LeadAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, Long> {

    List<LeadAssignment> findByAgentId(Long agentId);
    List<LeadAssignment> findByTeamId(Long teamId);

    List<LeadAssignment> findByLeadId(Long leadId);
    void deleteByLeadId(Long leadId);
    Optional<LeadAssignment> findByLeadIdAndStatus(
            Long leadId,
            LeadAssignment.AssignmentStatus status
    );
    List<LeadAssignment> findByLeadIdAndStatusOrderByAssignedAtDesc(
            Long leadId,
            LeadAssignment.AssignmentStatus status
    );

    List<LeadAssignment> findByStatus(
            LeadAssignment.AssignmentStatus status
    );
    List<LeadAssignment> findByAgentIdAndStatus(
            Long agentId,
            LeadAssignment.AssignmentStatus status
    );
    List<LeadAssignment> findByTeamIdAndStatus(
            Long teamId,
            LeadAssignment.AssignmentStatus status
    );
}