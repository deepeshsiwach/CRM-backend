
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
            throw new IllegalArgumentException("Agent ID is required.");
        }

        LocalDate today = LocalDate.now(INDIA_ZONE);
        LocalDateTime currentTime = LocalDateTime.now(INDIA_ZONE);

        Optional<AgentAttendance> existing =
                attendanceRepository.findByAgentIdAndAttendanceDate(
                        agentId, today
                );

        if (existing.isPresent()) {
            AgentAttendance attendance = existing.get();

            /*
             * Do not overwrite the first login of the day.
             * If already logged out, clear logout time to mark
             * the agent as logged in again.
             */
            if (attendance.getLogoutTime() != null) {
                attendance.setLogoutTime(null);
                return attendanceRepository.save(attendance);
            }

            // Already logged in: do not create another record.
            return attendance;
        }

        AgentAttendance attendance = new AgentAttendance();
        attendance.setAgentId(agentId);
        attendance.setAttendanceDate(today);
        attendance.setLoginTime(currentTime);
        attendance.setLogoutTime(null);

        return attendanceRepository.save(attendance);
    }

    // RECORD AGENT LOGOUT
    @Transactional
    public AgentAttendance recordLogout(Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID is required.");
        }

        LocalDate today = LocalDate.now(INDIA_ZONE);

        AgentAttendance attendance =
                attendanceRepository.findByAgentIdAndAttendanceDate(
                        agentId, today
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "No attendance record found for today."
                        )
                );

        // Repeated logout requests must not change the saved time.
        if (attendance.getLogoutTime() != null) {
            return attendance;
        }

        attendance.setLogoutTime(LocalDateTime.now(INDIA_ZONE));

        return attendanceRepository.save(attendance);
    }

    // GET TODAY'S ATTENDANCE
    @Transactional(readOnly = true)
    public Optional<AgentAttendance> getTodayAttendance(Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID is required.");
        }

        LocalDate today = LocalDate.now(INDIA_ZONE);

        return attendanceRepository.findByAgentIdAndAttendanceDate(
                agentId, today
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
    public List<AgentAttendance> getAttendanceByDate(LocalDate date) {

        if (date == null) {
            throw new IllegalArgumentException("Attendance date is required.");
        }

        return attendanceRepository
                .findByAttendanceDateOrderByLoginTimeAsc(date);
    }
}
