
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

// ========================================
// RECORD AGENT LOGIN
// ========================================


    @Transactional
    public AgentAttendance recordLogin(Long agentId) {

        LocalDate today = LocalDate.now(INDIA_ZONE);
        LocalDateTime currentTime = LocalDateTime.now(INDIA_ZONE);

        Optional<AgentAttendance> existingAttendance =
                attendanceRepository.findByAgentIdAndAttendanceDate(
                        agentId, today
                );

        if (existingAttendance.isPresent()) {

            AgentAttendance attendance = existingAttendance.get();

            // Preserve the first login time of the day.
            // Clear the previous logout time when the agent logs in again.
            if (attendance.getLogoutTime() != null) {
                attendance.setLogoutTime(null);
                return attendanceRepository.save(attendance);
            }

            // Already logged in: keep the existing record.
            return attendance;
        }

        // No record exists for today, so save the first login.
        AgentAttendance attendance = new AgentAttendance();
        attendance.setAgentId(agentId);
        attendance.setAttendanceDate(today);
        attendance.setLoginTime(currentTime);
        attendance.setLogoutTime(null);

        return attendanceRepository.save(attendance);
    }


    // ========================================
    // RECORD AGENT LOGOUT
    // ========================================

    @Transactional
    public AgentAttendance recordLogout(Long agentId) {

        LocalDate today = LocalDate.now(INDIA_ZONE);

        AgentAttendance attendance =
                attendanceRepository.findByAgentIdAndAttendanceDate(
                        agentId, today
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "No attendance record found for today."
                        )
                );

        // Do not overwrite an existing logout time.
        if (attendance.getLogoutTime() != null) {
            return attendance;
        }

        attendance.setLogoutTime(
                LocalDateTime.now(INDIA_ZONE)
        );

        return attendanceRepository.save(attendance);
    }

    // ========================================
    // GET TODAY'S ATTENDANCE
    // ========================================

    public Optional<AgentAttendance> getTodayAttendance(
            Long agentId) {

        LocalDate today = LocalDate.now(INDIA_ZONE);

        return attendanceRepository.findByAgentIdAndAttendanceDate(
                agentId, today
        );
    }

    // ========================================
    // GET ALL ATTENDANCE FOR ADMIN
    // ========================================

    public List<AgentAttendance> getAllAttendance() {

        return attendanceRepository
                .findAllByOrderByAttendanceDateDescLoginTimeAsc();
    }

    // ========================================
    // GET ATTENDANCE BY DATE
    // ========================================

    public List<AgentAttendance> getAttendanceByDate(
            LocalDate date) {

        return attendanceRepository
                .findByAttendanceDateOrderByLoginTimeAsc(date);
    }
}
