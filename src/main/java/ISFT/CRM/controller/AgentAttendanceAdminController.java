
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
import java.util.Comparator;
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

    @GetMapping
    public List<AttendanceRow> getAllAttendance() {

        Map<Long, String> agentNames = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == User.Role.AGENT)
                .collect(Collectors.toMap(
                        User::getId,
                        User::getFullName,
                        (first, second) -> first
                ));

        return attendanceRepository.findAll()
                .stream()
                .sorted(
                        Comparator
                                .comparing(
                                        AgentAttendance::getAttendanceDate,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                                .thenComparing(
                                        AgentAttendance::getLoginTime,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                )
                .map(attendance -> buildAttendanceRow(
                        attendance,
                        agentNames.getOrDefault(
                                attendance.getAgentId(),
                                "Agent #" + attendance.getAgentId()
                        )
                ))
                .collect(Collectors.toList());
    }

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

        String agentName = userRepository
                .findById(attendance.getAgentId())
                .map(User::getFullName)
                .orElse("Agent #" + attendance.getAgentId());

        List<AgentBreak> breaks =
                breakRepository
                        .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                                attendance.getAgentId(),
                                attendance.getId()
                        );

        long normalBreakSeconds = breaks.stream()
                .filter(b -> b.getBreakType()
                        == AgentBreak.BreakType.NORMAL)
                .mapToLong(b -> getEffectiveDuration(b, attendance))
                .sum();

        long exceptionBreakSeconds = breaks.stream()
                .filter(b -> b.getBreakType()
                        == AgentBreak.BreakType.EXCEPTION)
                .mapToLong(b -> getEffectiveDuration(b, attendance))
                .sum();

        long grossSeconds = getGrossWorkingSeconds(attendance);

        long netWorkingSeconds = Math.max(
                0,
                grossSeconds
                        - normalBreakSeconds
                        - exceptionBreakSeconds
        );

        return new AttendanceDetails(
                attendance.getId(),
                attendance.getAgentId(),
                agentName,
                attendance.getAttendanceDate() == null
                        ? null : attendance.getAttendanceDate().toString(),
                attendance.getLoginTime() == null
                        ? null : attendance.getLoginTime().toString(),
                attendance.getLogoutTime() == null
                        ? null : attendance.getLogoutTime().toString(),
                grossSeconds,
                netWorkingSeconds,
                normalBreakSeconds,
                exceptionBreakSeconds,
                breaks.stream()
                        .map(b -> toBreakRow(b, attendance))
                        .collect(Collectors.toList())
        );
    }

    private AttendanceRow buildAttendanceRow(
            AgentAttendance attendance,
            String agentName) {

        List<AgentBreak> breaks =
                breakRepository
                        .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                                attendance.getAgentId(),
                                attendance.getId()
                        );

        long normalBreakSeconds = breaks.stream()
                .filter(b -> b.getBreakType()
                        == AgentBreak.BreakType.NORMAL)
                .mapToLong(b -> getEffectiveDuration(b, attendance))
                .sum();

        long exceptionBreakSeconds = breaks.stream()
                .filter(b -> b.getBreakType()
                        == AgentBreak.BreakType.EXCEPTION)
                .mapToLong(b -> getEffectiveDuration(b, attendance))
                .sum();

        long grossSeconds = getGrossWorkingSeconds(attendance);

        long netWorkingSeconds = Math.max(
                0,
                grossSeconds
                        - normalBreakSeconds
                        - exceptionBreakSeconds
        );

        return new AttendanceRow(
                attendance.getId(),
                attendance.getAgentId(),
                agentName,
                attendance.getAttendanceDate() == null
                        ? null : attendance.getAttendanceDate().toString(),
                attendance.getLoginTime() == null
                        ? null : attendance.getLoginTime().toString(),
                attendance.getLogoutTime() == null
                        ? null : attendance.getLogoutTime().toString(),
                netWorkingSeconds,
                normalBreakSeconds,
                exceptionBreakSeconds
        );
    }

    /*
     * WORKING TIME
     * A record can never accumulate working time past midnight
     * on its attendance date.
     */
    private long getGrossWorkingSeconds(
            AgentAttendance attendance) {

        if (attendance.getLoginTime() == null
                || attendance.getAttendanceDate() == null) {
            return 0;
        }

        LocalDate attendanceDate = attendance.getAttendanceDate();
        LocalDateTime start = attendance.getLoginTime();

        LocalDateTime dayStart = attendanceDate.atStartOfDay();
        LocalDateTime dayEnd = attendanceDate
                .plusDays(1)
                .atStartOfDay();

        // Do not calculate time before the attendance date.
        if (start.isBefore(dayStart)) {
            start = dayStart;
        }

        // For a logged-in agent, use the current India time.
        // For a logged-out agent, use the recorded logout time.
        LocalDateTime end = attendance.getLogoutTime() != null
                ? attendance.getLogoutTime()
                : LocalDateTime.now(INDIA_ZONE);

        // Never let a record extend beyond its attendance day.
        if (end.isAfter(dayEnd)) {
            end = dayEnd;
        }

        // Do not calculate time before login or after logout.
        if (!end.isAfter(start)) {
            return 0;
        }

        return Duration.between(start, end).getSeconds();
    }

    /*
     * BREAK TIME
     * Active breaks stop accumulating at midnight too.
     */
    private long getEffectiveDuration(
            AgentBreak agentBreak,
            AgentAttendance attendance) {

        if (agentBreak.getStartTime() == null
                || attendance.getAttendanceDate() == null) {
            return 0;
        }

        LocalDateTime start = agentBreak.getStartTime();

        LocalDateTime dayEnd = attendance.getAttendanceDate()
                .plusDays(1)
                .atStartOfDay();

        if (!start.isBefore(dayEnd)) {
            return 0;
        }

        LocalDateTime end = agentBreak.getEndTime() != null
                ? agentBreak.getEndTime()
                : LocalDateTime.now(INDIA_ZONE);

        if (end.isAfter(dayEnd)) {
            end = dayEnd;
        }

        if (!end.isAfter(start)) {
            return 0;
        }

        long calculatedSeconds =
                Duration.between(start, end).getSeconds();

        // If a break has already ended, respect its recorded
        // duration, but never allow it beyond the attendance day.
        if (agentBreak.getEndTime() != null
                && agentBreak.getDurationSeconds() != null) {

            return Math.min(
                    Math.max(0, agentBreak.getDurationSeconds()),
                    calculatedSeconds
            );
        }

        return calculatedSeconds;
    }

    private BreakRow toBreakRow(
            AgentBreak agentBreak,
            AgentAttendance attendance) {

        return new BreakRow(
                agentBreak.getId(),
                agentBreak.getBreakType() == null
                        ? null : agentBreak.getBreakType().name(),
                agentBreak.getStartTime() == null
                        ? null : agentBreak.getStartTime().toString(),
                agentBreak.getEndTime() == null
                        ? null : agentBreak.getEndTime().toString(),
                getEffectiveDuration(agentBreak, attendance),
                agentBreak.getReason()
        );
    }

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
        public long getExceptionBreakSeconds() {
            return exceptionBreakSeconds;
        }
    }

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
        public long getGrossWorkingSeconds() {
            return grossWorkingSeconds;
        }
        public long getWorkingSeconds() { return workingSeconds; }
        public long getNormalBreakSeconds() {
            return normalBreakSeconds;
        }
        public long getExceptionBreakSeconds() {
            return exceptionBreakSeconds;
        }
        public List<BreakRow> getBreaks() { return breaks; }
    }

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
