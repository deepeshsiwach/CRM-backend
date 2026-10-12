
        package ISFT.CRM.service;

import ISFT.CRM.entity.AgentAttendance;
import ISFT.CRM.repository.AgentAttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class AgentAttendanceService {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final AgentAttendanceRepository attendanceRepository;

    public AgentAttendanceService(
            AgentAttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    // RECORD AGENT LOGIN
    @Transactional
    public AgentAttendance recordLogin(Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException(
                    "Agent ID is required."
            );
        }

        LocalDateTime now = LocalDateTime.now(INDIA_ZONE);

        // If the agent already has an open session, reuse it.
        Optional<AgentAttendance> active =
                attendanceRepository
                        .findFirstByAgentIdAndLogoutTimeIsNullOrderByLoginTimeDesc(
                                agentId
                        );

        if (active.isPresent()) {
            AgentAttendance attendance = active.get();

            // Reuse only today's active attendance session.
            if (now.toLocalDate().equals(
                    attendance.getAttendanceDate())) {
                return attendance;
            }

            // Close a stale session at the end of its attendance day.
            if (attendance.getAttendanceDate() != null) {
                LocalDateTime dayEnd = attendance
                        .getAttendanceDate()
                        .plusDays(1)
                        .atStartOfDay();

                attendance.setLogoutTime(dayEnd);
                attendanceRepository.save(attendance);
            }
        }
        // Create a new attendance session.
        AgentAttendance attendance = new AgentAttendance();
        attendance.setAgentId(agentId);
        attendance.setAttendanceDate(now.toLocalDate());
        attendance.setLoginTime(now);
        attendance.setLogoutTime(null);

        return attendanceRepository.save(attendance);
    }

    // RECORD AGENT LOGOUT
    @Transactional
    public AgentAttendance recordLogout(Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException(
                    "Agent ID is required."
            );
        }

        AgentAttendance attendance =
                attendanceRepository
                        .findFirstByAgentIdAndLogoutTimeIsNullOrderByLoginTimeDesc(
                                agentId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No active attendance session found."
                                )
                        );

        attendance.setLogoutTime(
                LocalDateTime.now(INDIA_ZONE)
        );

        return attendanceRepository.save(attendance);
    }

    // GET TODAY'S LATEST ATTENDANCE SESSION
    @Transactional(readOnly = true)
    public Optional<AgentAttendance> getTodayAttendance(
            Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException(
                    "Agent ID is required."
            );
        }

        LocalDate today = LocalDate.now(INDIA_ZONE);

        return attendanceRepository
                .findFirstByAgentIdAndAttendanceDateOrderByLoginTimeDesc(
                        agentId,
                        today
                );
    }

    // GET ALL ATTENDANCE FOR ADMIN
    @Transactional(readOnly = true)
    public List<AgentAttendance> getAllAttendance() {

        return attendanceRepository
                .findAllByOrderByAttendanceDateDescLoginTimeAsc();
    }

    // GET ATTENDANCE BY DATE
    @Transactional(readOnly = true)
    public List<AgentAttendance> getAttendanceByDate(
            LocalDate date) {

        if (date == null) {
            throw new IllegalArgumentException(
                    "Attendance date is required."
            );
        }

        return attendanceRepository
                .findByAttendanceDateOrderByLoginTimeAsc(date);
    }
}