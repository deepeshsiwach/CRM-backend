
package ISFT.CRM.controller;

import ISFT.CRM.entity.AgentAttendance;
import ISFT.CRM.entity.AgentBreak;
import ISFT.CRM.entity.User;
import ISFT.CRM.repository.AgentAttendanceRepository;
import ISFT.CRM.repository.AgentBreakRepository;
import ISFT.CRM.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
                .filter(b -> {
                    AgentBreak.BreakType type = b.getBreakType();
                    return type == AgentBreak.BreakType.NORMAL
                            || type == AgentBreak.BreakType.LUNCH
                            || type == AgentBreak.BreakType.TEA
                            || type == AgentBreak.BreakType.BIO;
                })
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
                .filter(b -> {
                    AgentBreak.BreakType type = b.getBreakType();
                    return type == AgentBreak.BreakType.NORMAL
                            || type == AgentBreak.BreakType.LUNCH
                            || type == AgentBreak.BreakType.TEA
                            || type == AgentBreak.BreakType.BIO;
                })
                .mapToLong(b -> {
                    // Completed normal break: use its effective duration.
                    if (b.getEndTime() != null) {
                        return getEffectiveDuration(b, attendance);
                    }

                    // Active normal break: calculate elapsed time up to now.
                    if (b.getStartTime() != null) {
                        LocalDateTime now = LocalDateTime.now(INDIA_ZONE);
                        LocalDateTime end = now;

                        LocalDateTime dayEnd = attendance.getAttendanceDate()
                                .plusDays(1)
                                .atStartOfDay();

                        if (end.isAfter(dayEnd)) {
                            end = dayEnd;
                        }

                        LocalDateTime start = b.getStartTime();
                        LocalDateTime dayStart = attendance.getAttendanceDate()
                                .atStartOfDay();

                        if (start.isBefore(dayStart)) {
                            start = dayStart;
                        }

                        return end.isAfter(start)
                                ? Duration.between(start, end).getSeconds()
                                : 0L;
                    }

                    return 0L;
                })
                .sum();

        long exceptionBreakSeconds = breaks.stream()
                .filter(b -> b.getBreakType()
                        == AgentBreak.BreakType.EXCEPTION)
                .mapToLong(b -> getEffectiveDuration(b, attendance))
                .sum();

        // Find the agent's currently active break.
        LocalDate today = LocalDate.now(INDIA_ZONE);

        AgentBreak activeBreak = null;

        if (attendance.getAttendanceDate() != null
                && attendance.getAttendanceDate().equals(today)
                && attendance.getLogoutTime() == null) {

            activeBreak = breaks.stream()
                    .filter(b -> b.getEndTime() == null)
                    .filter(b -> b.getStartTime() != null)
                    .max(Comparator.comparing(AgentBreak::getStartTime))
                    .orElse(null);
        }

        boolean onBreak = activeBreak != null;

        String activeBreakType =
                onBreak && activeBreak.getBreakType() != null
                        ? activeBreak.getBreakType().name()
                        : null;

        String activeBreakStartTime =
                onBreak && activeBreak.getStartTime() != null
                        ? activeBreak.getStartTime().toString()
                        : null;

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
                exceptionBreakSeconds,
                onBreak,
                activeBreakType,
                activeBreakStartTime
        );
    }

    // WORKING TIME
    private long getGrossWorkingSeconds(
            AgentAttendance attendance) {

        if (attendance.getLoginTime() == null
                || attendance.getAttendanceDate() == null) {
            return 0;
        }

        LocalDate attendanceDate = attendance.getAttendanceDate();
        LocalDate today = LocalDate.now(INDIA_ZONE);

        LocalDateTime dayStart = attendanceDate.atStartOfDay();
        LocalDateTime dayEnd =
                attendanceDate.plusDays(1).atStartOfDay();

        LocalDateTime start = attendance.getLoginTime();

        if (start.isBefore(dayStart)) {
            start = dayStart;
        }

        LocalDateTime end;

        if (attendance.getLogoutTime() != null) {
            end = attendance.getLogoutTime();
        } else if (attendanceDate.isBefore(today)) {
            end = dayEnd;
        } else if (attendanceDate.isAfter(today)) {
            return 0;
        } else {
            end = LocalDateTime.now(INDIA_ZONE);
        }

        if (end.isAfter(dayEnd)) {
            end = dayEnd;
        }

        if (!end.isAfter(start)) {
            return 0;
        }

        return Duration.between(start, end).getSeconds();
    }

    // BREAK TIME
    private long getEffectiveDuration(
            AgentBreak agentBreak,
            AgentAttendance attendance) {

        if (agentBreak.getStartTime() == null
                || attendance.getAttendanceDate() == null) {
            return 0;
        }

        LocalDateTime dayStart =
                attendance.getAttendanceDate().atStartOfDay();

        LocalDateTime dayEnd =
                attendance.getAttendanceDate()
                        .plusDays(1)
                        .atStartOfDay();

        LocalDateTime start = agentBreak.getStartTime();

        if (start.isBefore(dayStart)) {
            start = dayStart;
        }

        if (!start.isBefore(dayEnd)) {
            return 0;
        }

        LocalDateTime end;

        if (agentBreak.getEndTime() != null) {
            end = agentBreak.getEndTime();
        } else if (attendance.getAttendanceDate()
                .isBefore(LocalDate.now(INDIA_ZONE))) {
            end = dayEnd;
        } else {
            end = LocalDateTime.now(INDIA_ZONE);
        }

        if (end.isAfter(dayEnd)) {
            end = dayEnd;
        }

        if (!end.isAfter(start)) {
            return 0;
        }

        long calculatedSeconds =
                Duration.between(start, end).getSeconds();

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

    // ATTENDANCE ROW DTO
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

        // New live-break fields
        private final boolean onBreak;
        private final String activeBreakType;
        private final String activeBreakStartTime;

        public AttendanceRow(
                Long id,
                Long agentId,
                String agentName,
                String attendanceDate,
                String loginTime,
                String logoutTime,
                long workingSeconds,
                long normalBreakSeconds,
                long exceptionBreakSeconds,
                boolean onBreak,
                String activeBreakType,
                String activeBreakStartTime) {

            this.id = id;
            this.agentId = agentId;
            this.agentName = agentName;
            this.attendanceDate = attendanceDate;
            this.loginTime = loginTime;
            this.logoutTime = logoutTime;
            this.workingSeconds = workingSeconds;
            this.normalBreakSeconds = normalBreakSeconds;
            this.exceptionBreakSeconds = exceptionBreakSeconds;
            this.onBreak = onBreak;
            this.activeBreakType = activeBreakType;
            this.activeBreakStartTime = activeBreakStartTime;
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
        public boolean isOnBreak() { return onBreak; }
        public String getActiveBreakType() { return activeBreakType; }
        public String getActiveBreakStartTime() {
            return activeBreakStartTime;
        }
    }

    // ATTENDANCE DETAILS DTO
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

    // BREAK DTO
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

    // DELETE ATTENDANCE RECORD (ADMIN)
    @DeleteMapping("/{attendanceId}")
    @Transactional
    public Map<String, String> deleteAttendance(
            @PathVariable Long attendanceId) {

        AgentAttendance attendance =
                attendanceRepository.findById(attendanceId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Attendance record not found."
                                )
                        );

        List<AgentBreak> breaks =
                breakRepository
                        .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                                attendance.getAgentId(),
                                attendance.getId()
                        );

        breakRepository.deleteAll(breaks);
        attendanceRepository.delete(attendance);

        return Map.of(
                "message",
                "Attendance record deleted successfully."
        );
    }
}
