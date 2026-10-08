package ISFT.CRM.repository;

import ISFT.CRM.entity.AgentBreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentBreakRepository
        extends JpaRepository<AgentBreak, Long> {

    Optional<AgentBreak> findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
            Long agentId
    );

    List<AgentBreak> findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
            Long agentId,
            Long attendanceId
    );
}