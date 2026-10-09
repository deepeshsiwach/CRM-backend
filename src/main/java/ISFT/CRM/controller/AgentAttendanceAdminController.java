
package ISFT.CRM.controller;

import ISFT.CRM.entity.AgentAttendance;
import ISFT.CRM.entity.AgentBreak;
import ISFT.CRM.entity.User;
import ISFT.CRM.repository.AgentAttendanceRepository;
import ISFT.CRM.repository.AgentBreakRepository;
import ISFT.CRM.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/attendance")
public class AgentAttendanceAdminController {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final AgentAttendanceRepository attendanceRepository;
    private final AgentBreakRepository breakRepository;
    private final UserRepository userRepository;

    public AgentAttendanceAdminController(
            AgentAttendanceRepository attendanceRepository,
            AgentBreakRepository breakRepository,
            UserRepository userRepository) {

        this.attendanceRepository = attendanceRepository;
        this.breakRepository = breakRepository;
        this.userRepository = userRepository;
    }

    // ==========================================================
    // GET ALL ATTENDANCE
    // ==========================================================

    @GetMapping
    public List<AttendanceRow> getAllAttendance() {

        Map<Long, String> agentNames =
                userRepository.findAll()
                        .stream()
                        .filter(user ->
                                user.getRole() == User.Role.AGENT)
                        .collect(Collectors.toMap(
                                User::getId,
                                User::getFullName,
                                (first, second) -> first
                        ));

        return attendanceRepository
                .findAll()
                .stream()
                .sorted((a, b) -> {
                    int dateCompare = b.getAttendanceDate()
                            .compareTo(a.getAttendanceDate());

                    if (dateCompare != 0) {
                        return dateCompare;
                    }

                    return a.getLoginTime()
                            .compareTo(b.getLoginTime());
                })
                .map(attendance ->
                        buildAttendanceRow(
                                attendance,
                                agentNames.getOrDefault(
                                        attendance.getAgentId(),
                                        "Agent #" + attendance.getAgentId()
                                )
                        )
                )
                .collect(Collectors.toList());
    }

    // ==========================================================
    // GET ATTENDANCE DETAILS
    // ==========================================================

    @GetMapping("/{attendanceId}")
    public AttendanceDetails getAttendanceDetails(
            @PathVariable Long attendanceId) {

        AgentAttendance attendance =
                attendanceRepository.findById(attendanceId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance record not found."
                                )
                        );

        String agentName =
                userRepository.findById(attendance.getAgentId())
                        .map(User::getFullName)
                        .orElse("Agent #" + attendance.getAgentId());

        List<AgentBreak> breaks =
                breakRepository
                        .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                                attendance.getAgentId(),
                                attendance.getId()
                        );

        long normalBreakSeconds =
                breaks.stream()
                        .filter(b ->
                                b.getBreakType()
                                        == AgentBreak.BreakType.NORMAL)
                        .mapToLong(b ->
                                getEffectiveDuration(b, attendance))
                        .sum();

        long exceptionBreakSeconds =
                breaks.stream()
                        .filter(b ->
                                b.getBreakType()
                                        == AgentBreak.BreakType.EXCEPTION)
                        .mapToLong(b ->
                                getEffectiveDuration(b, attendance))
                        .sum();

        long grossSeconds =
                getGrossWorkingSeconds(attendance);

        long netWorkingSeconds =
                Math.max(
                        0,
                        grossSeconds
                                - normalBreakSeconds
                                - exceptionBreakSeconds
                );

        return new AttendanceDetails(
                attendance.getId(),
                attendance.getAgentId(),
                agentName,
                attendance.getAttendanceDate().toString(),
                attendance.getLoginTime() == null
                        ? null
                        : attendance.getLoginTime().toString(),
                attendance.getLogoutTime() == null
                        ? null
                        : attendance.getLogoutTime().toString(),
                grossSeconds,
                netWorkingSeconds,
                normalBreakSeconds,
                exceptionBreakSeconds,
                breaks.stream()
                        .map(b -> toBreakRow(b, attendance))
                        .collect(Collectors.toList())
        );
    }

    // ==========================================================
    // BUILD ATTENDANCE ROW
    // ==========================================================

    private AttendanceRow buildAttendanceRow(
            AgentAttendance attendance,
            String agentName) {

        List<AgentBreak> breaks =
                breakRepository
                        .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                                attendance.getAgentId(),
                                attendance.getId()
                        );

        long normalBreakSeconds =
                breaks.stream()
                        .filter(b ->
                                b.getBreakType()
                                        == AgentBreak.BreakType.NORMAL)
                        .mapToLong(b ->
                                getEffectiveDuration(b, attendance))
                        .sum();

        long exceptionBreakSeconds =
                breaks.stream()
                        .filter(b ->
                                b.getBreakType()
                                        == AgentBreak.BreakType.EXCEPTION)
                        .mapToLong(b ->
                                getEffectiveDuration(b, attendance))
                        .sum();

        long grossSeconds =
                getGrossWorkingSeconds(attendance);

        long netWorkingSeconds =
                Math.max(
                        0,
                        grossSeconds
                                - normalBreakSeconds
                                - exceptionBreakSeconds
                );

        return new AttendanceRow(
                attendance.getId(),
                attendance.getAgentId(),
                agentName,
                attendance.getAttendanceDate().toString(),
                attendance.getLoginTime() == null
                        ? null
                        : attendance.getLoginTime().toString(),
                attendance.getLogoutTime() == null
                        ? null
                        : attendance.getLogoutTime().toString(),
                netWorkingSeconds,
                normalBreakSeconds,
                exceptionBreakSeconds
        );
    }

    // ==========================================================
    // GROSS WORKING TIME
    // ==========================================================

    private long getGrossWorkingSeconds(
            AgentAttendance attendance) {

        if (attendance.getLoginTime() == null
                || attendance.getAttendanceDate() == null) {
            return 0;
        }

        LocalDate attendanceDate =
                attendance.getAttendanceDate();

        LocalDateTime startTime =
                attendance.getLoginTime();

        LocalDateTime endOfAttendanceDay =
                attendanceDate.plusDays(1).atStartOfDay();

        LocalDateTime endTime;

        if (attendance.getLogoutTime() != null) {

            endTime = attendance.getLogoutTime();

        } else if (attendanceDate.equals(
                LocalDate.now(INDIA_ZONE))) {

            endTime = LocalDateTime.now(INDIA_ZONE);

        } else {

            // Previous day's attendance must never keep
            // counting into later days.
            endTime = endOfAttendanceDay;
        }

        // Never count outside this attendance date.
        if (endTime.isAfter(endOfAttendanceDay)) {
            endTime = endOfAttendanceDay;
        }

        if (endTime.isBefore(startTime)) {
            return 0;
        }

        return Math.max(
                0,
                Duration.between(startTime, endTime).getSeconds()
        );
    }

    // ==========================================================
    // BREAK DURATION
    // ==========================================================

    private long getEffectiveDuration(
            AgentBreak agentBreak,
            AgentAttendance attendance) {

        if (agentBreak.getStartTime() == null
                || attendance.getAttendanceDate() == null) {
            return 0;
        }

        LocalDate attendanceDate =
                attendance.getAttendanceDate();

        LocalDateTime startTime =
                agentBreak.getStartTime();

        LocalDateTime endOfAttendanceDay =
                attendanceDate.plusDays(1).atStartOfDay();

        LocalDateTime endTime;

        if (agentBreak.getEndTime() != null) {

            endTime = agentBreak.getEndTime();

        } else if (attendanceDate.equals(
                LocalDate.now(INDIA_ZONE))) {

            endTime = LocalDateTime.now(INDIA_ZONE);

        } else {

            // A break left open on a previous date stops
            // at the end of its attendance day.
            endTime = endOfAttendanceDay;
        }

        // Limit the break to its attendance date.
        if (endTime.isAfter(endOfAttendanceDay)) {
            endTime = endOfAttendanceDay;
        }

        if (endTime.isBefore(startTime)) {
            return 0;
        }

        // Calculate from timestamps instead of relying on a
        // stored duration that might be stale or incorrect.
        return Math.max(
                0,
                Duration.between(startTime, endTime).getSeconds()
        );
    }

    // ==========================================================
    // BREAK ROW
    // ==========================================================

    private BreakRow toBreakRow(
            AgentBreak agentBreak,
            AgentAttendance attendance) {

        return new BreakRow(
                agentBreak.getId(),
                agentBreak.getBreakType() == null
                        ? null
                        : agentBreak.getBreakType().name(),
                agentBreak.getStartTime() == null
                        ? null
                        : agentBreak.getStartTime().toString(),
                agentBreak.getEndTime() == null
                        ? null
                        : agentBreak.getEndTime().toString(),
                getEffectiveDuration(agentBreak, attendance),
                agentBreak.getReason()
        );
    }

    // ==========================================================
    // ATTENDANCE ROW DTO
    // ==========================================================

    public static class AttendanceRow {

        private final Long id;
        private final Long agentId;
        private final String agentName;
        private final String attendanceDate;
        private final String loginTime;
        private final String logoutTime;
        private final long workingSeconds;
        private final long normalBreakSeconds;
        private final long exceptionBreakSeconds;

        public AttendanceRow(
                Long id,
                Long agentId,
                String agentName,
                String attendanceDate,
                String loginTime,
                String logoutTime,
                long workingSeconds,
                long normalBreakSeconds,
                long exceptionBreakSeconds) {

            this.id = id;
            this.agentId = agentId;
            this.agentName = agentName;
            this.attendanceDate = attendanceDate;
            this.loginTime = loginTime;
            this.logoutTime = logoutTime;
            this.workingSeconds = workingSeconds;
            this.normalBreakSeconds = normalBreakSeconds;
            this.exceptionBreakSeconds = exceptionBreakSeconds;
        }

        public Long getId() { return id; }
        public Long getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public String getAttendanceDate() { return attendanceDate; }
        public String getLoginTime() { return loginTime; }
        public String getLogoutTime() { return logoutTime; }
        public long getWorkingSeconds() { return workingSeconds; }
        public long getNormalBreakSeconds() { return normalBreakSeconds; }
        public long getExceptionBreakSeconds() { return exceptionBreakSeconds; }
    }

    // ==========================================================
    // ATTENDANCE DETAILS DTO
    // ==========================================================

    public static class AttendanceDetails {

        private final Long id;
        private final Long agentId;
        private final String agentName;
        private final String attendanceDate;
        private final String loginTime;
        private final String logoutTime;
        private final long grossWorkingSeconds;
        private final long workingSeconds;
        private final long normalBreakSeconds;
        private final long exceptionBreakSeconds;
        private final List<BreakRow> breaks;

        public AttendanceDetails(
                Long id,
                Long agentId,
                String agentName,
                String attendanceDate,
                String loginTime,
                String logoutTime,
                long grossWorkingSeconds,
                long workingSeconds,
                long normalBreakSeconds,
                long exceptionBreakSeconds,
                List<BreakRow> breaks) {

            this.id = id;
            this.agentId = agentId;
            this.agentName = agentName;
            this.attendanceDate = attendanceDate;
            this.loginTime = loginTime;
            this.logoutTime = logoutTime;
            this.grossWorkingSeconds = grossWorkingSeconds;
            this.workingSeconds = workingSeconds;
            this.normalBreakSeconds = normalBreakSeconds;
            this.exceptionBreakSeconds = exceptionBreakSeconds;
            this.breaks = breaks;
        }

        public Long getId() { return id; }
        public Long getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public String getAttendanceDate() { return attendanceDate; }
        public String getLoginTime() { return loginTime; }
        public String getLogoutTime() { return logoutTime; }
        public long getGrossWorkingSeconds() { return grossWorkingSeconds; }
        public long getWorkingSeconds() { return workingSeconds; }
        public long getNormalBreakSeconds() { return normalBreakSeconds; }
        public long getExceptionBreakSeconds() { return exceptionBreakSeconds; }
        public List<BreakRow> getBreaks() { return breaks; }
    }

    // ==========================================================
    // BREAK DTO
    // ==========================================================

    public static class BreakRow {

        private final Long id;
        private final String breakType;
        private final String startTime;
        private final String endTime;
        private final long durationSeconds;
        private final String reason;

        public BreakRow(
                Long id,
                String breakType,
                String startTime,
                String endTime,
                long durationSeconds,
                String reason) {

            this.id = id;
            this.breakType = breakType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationSeconds = durationSeconds;
            this.reason = reason;
        }

        public Long getId() { return id; }
        public String getBreakType() { return breakType; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public long getDurationSeconds() { return durationSeconds; }
        public String getReason() { return reason; }
    }
}
