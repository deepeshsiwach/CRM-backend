
        package ISFT.CRM.repository;

import ISFT.CRM.entity.AgentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgentAttendanceRepository
        extends JpaRepository<AgentAttendance, Long> {

    // Find the latest attendance session for an agent on a date
    Optional<AgentAttendance>
    findFirstByAgentIdAndAttendanceDateOrderByLoginTimeDesc(
            Long agentId,
            LocalDate attendanceDate
    );

    // Find the currently open attendance session for an agent
    Optional<AgentAttendance>
    findFirstByAgentIdAndLogoutTimeIsNullOrderByLoginTimeDesc(
            Long agentId
    );

    // Get all attendance sessions for a date
    List<AgentAttendance>
    findByAttendanceDateOrderByLoginTimeAsc(
            LocalDate attendanceDate
    );

    // Get all attendance records, newest dates first
    List<AgentAttendance>
    findAllByOrderByAttendanceDateDescLoginTimeAsc();
}
