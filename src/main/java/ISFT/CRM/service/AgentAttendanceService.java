
package ISFT.CRM.service;

import ISFT.CRM.entity.AgentAttendance;
import ISFT.CRM.repository.AgentAttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
                attendanceRepository.findByAgentIdAndAttendanceDate(
                        agentId, today
                );

        if (existingAttendance.isPresent()) {

            AgentAttendance attendance = existingAttendance.get();

            if (attendance.getLogoutTime() == null) {
                return attendance;
            }

            attendance.setLoginTime(LocalDateTime.now());
            attendance.setLogoutTime(null);

            return attendanceRepository.save(attendance);
        }

        AgentAttendance attendance = new AgentAttendance();
        attendance.setAgentId(agentId);
        attendance.setAttendanceDate(today);
        attendance.setLoginTime(LocalDateTime.now());
        attendance.setLogoutTime(null);

        return attendanceRepository.save(attendance);
    }

    // ========================================
    // RECORD AGENT LOGOUT
    // ========================================

    @Transactional
    public AgentAttendance recordLogout(Long agentId) {

        LocalDate today = LocalDate.now();

        AgentAttendance attendance =
                attendanceRepository.findByAgentIdAndAttendanceDate(
                        agentId, today
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "No attendance record found for today."
                        )
                );

        if (attendance.getLogoutTime() != null) {
            return attendance;
        }

        attendance.setLogoutTime(LocalDateTime.now());

        return attendanceRepository.save(attendance);
    }

    // ========================================
    // GET TODAY'S ATTENDANCE
    // ========================================

    public Optional<AgentAttendance> getTodayAttendance(Long agentId) {

        return attendanceRepository.findByAgentIdAndAttendanceDate(
                agentId, LocalDate.now()
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

    public List<AgentAttendance> getAttendanceByDate(LocalDate date) {

        return attendanceRepository
                .findByAttendanceDateOrderByLoginTimeAsc(date);
    }
}
