
package ISFT.CRM.repository;

import ISFT.CRM.entity.AgentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgentAttendanceRepository
        extends JpaRepository<AgentAttendance, Long> {

    Optional<AgentAttendance> findByAgentIdAndAttendanceDate(
            Long agentId,
            LocalDate attendanceDate
    );

    List<AgentAttendance> findByAttendanceDateOrderByLoginTimeAsc(
            LocalDate attendanceDate
    );

    List<AgentAttendance> findAllByOrderByAttendanceDateDescLoginTimeAsc();
}
