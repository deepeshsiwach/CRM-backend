package ISFT.CRM.service;

import ISFT.CRM.entity.AgentAttendance;
import ISFT.CRM.repository.AgentAttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AgentAttendanceService {

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

        LocalDate today = LocalDate.now();

        Optional<AgentAttendance> existingAttendance =
                attendanceRepository
                        .findByAgentIdAndAttendanceDate(
                                agentId,
                                today
                        );

        // ------------------------------------
        // If today's record already exists
        // ------------------------------------

        if (existingAttendance.isPresent()) {

            AgentAttendance attendance =
                    existingAttendance.get();

            // If the agent is already logged in,
            // do not create another record.
            if (attendance.getLogoutTime() == null) {
                return attendance;
            }

            // If there was an earlier login/logout
            // today, start a new login session by
            // updating the login time.
            attendance.setLoginTime(
                    LocalDateTime.now()
            );

            attendance.setLogoutTime(null);

            return attendanceRepository.save(attendance);
        }


        // ------------------------------------
        // Create today's attendance record
        // ------------------------------------

        AgentAttendance attendance =
                new AgentAttendance();

        attendance.setAgentId(agentId);

        attendance.setAttendanceDate(today);

        attendance.setLoginTime(
                LocalDateTime.now()
        );

        attendance.setLogoutTime(null);

        return attendanceRepository.save(attendance);
    }


    // ========================================
    // RECORD AGENT LOGOUT
    // ========================================

    @Transactional
    public AgentAttendance recordLogout(Long agentId) {

        LocalDate today = LocalDate.now();

        Optional<AgentAttendance> existingAttendance =
                attendanceRepository
                        .findByAgentIdAndAttendanceDate(
                                agentId,
                                today
                        );

        // ------------------------------------
        // No attendance record found
        // ------------------------------------

        if (existingAttendance.isEmpty()) {

            throw new IllegalStateException(
                    "No attendance record found for today."
            );
        }


        AgentAttendance attendance =
                existingAttendance.get();


        // ------------------------------------
        // Already logged out
        // ------------------------------------

        if (attendance.getLogoutTime() != null) {

            return attendance;
        }


        // ------------------------------------
        // Record logout time
        // ------------------------------------

        attendance.setLogoutTime(
                LocalDateTime.now()
        );

        return attendanceRepository.save(attendance);
    }


    // ========================================
    // GET TODAY'S ATTENDANCE
    // ========================================

    public Optional<AgentAttendance> getTodayAttendance(
            Long agentId) {

        return attendanceRepository
                .findByAgentIdAndAttendanceDate(
                        agentId,
                        LocalDate.now()
                );
    }
}