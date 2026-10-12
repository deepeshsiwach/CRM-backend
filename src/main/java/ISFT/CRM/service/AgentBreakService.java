 package ISFT.CRM.service;

import ISFT.CRM.entity.AgentAttendance;
import ISFT.CRM.entity.AgentBreak;
import ISFT.CRM.repository.AgentAttendanceRepository;
import ISFT.CRM.repository.AgentBreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AgentBreakService {

    private static final long NORMAL_BREAK_LIMIT_SECONDS = 3600;

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final AgentBreakRepository breakRepository;
    private final AgentAttendanceRepository attendanceRepository;

    public AgentBreakService(
            AgentBreakRepository breakRepository,
            AgentAttendanceRepository attendanceRepository) {
        this.breakRepository = breakRepository;
        this.attendanceRepository = attendanceRepository;
    }

    // START BREAK
    @Transactional
    public AgentBreak startBreak(
            Long agentId,
            AgentBreak.BreakType breakType,
            String reason) {

        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID is required.");
        }

        if (breakType == null) {
            throw new IllegalArgumentException("Break type is required.");
        }

        if (breakType == AgentBreak.BreakType.EXCEPTION
                && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException(
                    "Reason is required for an exception break."
            );
        }

        AgentAttendance attendance =
                attendanceRepository
                        .findFirstByAgentIdAndLogoutTimeIsNullOrderByLoginTimeDesc(
                                agentId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No active attendance session. Please log in first."
                                )
                        );

        AgentBreak activeBreak =
                breakRepository
                        .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                                agentId
                        )
                        .orElse(null);

        if (activeBreak != null) {

            // Close an orphaned break belonging to another session.
            if (!activeBreak.getAttendanceId().equals(attendance.getId())) {
                closeBreak(activeBreak, LocalDateTime.now(INDIA_ZONE));
            } else {
                throw new IllegalStateException(
                        "You already have an active break."
                );
            }
        }

        AgentBreak agentBreak = new AgentBreak();
        agentBreak.setAgentId(agentId);
        agentBreak.setAttendanceId(attendance.getId());
        agentBreak.setBreakType(breakType);
        agentBreak.setStartTime(LocalDateTime.now(INDIA_ZONE));
        agentBreak.setReason(
                reason == null || reason.trim().isEmpty()
                        ? null
                        : reason.trim()
        );
        agentBreak.setDurationSeconds(null);
        agentBreak.setLimitAlertSent(false);

        return breakRepository.save(agentBreak);
    }

    // END BREAK
    @Transactional
    public AgentBreak endBreak(Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID is required.");
        }

        AgentBreak agentBreak =
                breakRepository
                        .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                                agentId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No active break found."
                                )
                        );

        closeBreak(agentBreak, LocalDateTime.now(INDIA_ZONE));

        return breakRepository.save(agentBreak);
    }

    // CLOSE THE ACTIVE BREAK FOR A PARTICULAR ATTENDANCE SESSION
    @Transactional
    public void closeActiveBreakForAttendance(
            Long agentId,
            Long attendanceId,
            LocalDateTime endTime) {

        if (agentId == null || attendanceId == null || endTime == null) {
            return;
        }

        AgentBreak activeBreak =
                breakRepository
                        .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                                agentId
                        )
                        .orElse(null);

        if (activeBreak != null
                && attendanceId.equals(activeBreak.getAttendanceId())) {

            closeBreak(activeBreak, endTime);
            breakRepository.save(activeBreak);
        }
    }

    private void closeBreak(
            AgentBreak agentBreak,
            LocalDateTime endTime) {

        LocalDateTime effectiveEnd =
                endTime.isBefore(agentBreak.getStartTime())
                        ? agentBreak.getStartTime()
                        : endTime;

        long durationSeconds =
                Math.max(
                        0,
                        Duration.between(
                                agentBreak.getStartTime(),
                                effectiveEnd
                        ).getSeconds()
                );

        agentBreak.setEndTime(effectiveEnd);
        agentBreak.setDurationSeconds(durationSeconds);
    }

    // GET ACTIVE BREAK
    @Transactional(readOnly = true)
    public AgentBreak getActiveBreak(Long agentId) {

        if (agentId == null) {
            return null;
        }

        return breakRepository
                .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                        agentId
                )
                .orElse(null);
    }

    // NORMAL BREAK TOTAL
    @Transactional(readOnly = true)
    public long getNormalBreakTotalSeconds(
            Long agentId,
            Long attendanceId) {

        return getBreaks(agentId, attendanceId).stream()
                .filter(b ->
                        b.getBreakType() == AgentBreak.BreakType.NORMAL
                )
                .mapToLong(this::getEffectiveDuration)
                .sum();
    }

    // EXCEPTION BREAK TOTAL
    @Transactional(readOnly = true)
    public long getExceptionBreakTotalSeconds(
            Long agentId,
            Long attendanceId) {

        return getBreaks(agentId, attendanceId).stream()
                .filter(b ->
                        b.getBreakType() == AgentBreak.BreakType.EXCEPTION
                )
                .mapToLong(this::getEffectiveDuration)
                .sum();
    }

    private List<AgentBreak> getBreaks(
            Long agentId,
            Long attendanceId) {

        return breakRepository
                .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                        agentId,
                        attendanceId
                );
    }

    private long getEffectiveDuration(AgentBreak agentBreak) {

        if (agentBreak.getDurationSeconds() != null) {
            return Math.max(0, agentBreak.getDurationSeconds());
        }

        if (agentBreak.getEndTime() == null) {
            return Math.max(
                    0,
                    Duration.between(
                            agentBreak.getStartTime(),
                            LocalDateTime.now(INDIA_ZONE)
                    ).getSeconds()
            );
        }

        return Math.max(
                0,
                Duration.between(
                        agentBreak.getStartTime(),
                        agentBreak.getEndTime()
                ).getSeconds()
        );
    }

    // CHECK NORMAL BREAK LIMIT
    @Transactional(readOnly = true)
    public boolean isNormalBreakOverLimit(
            Long agentId,
            Long attendanceId) {

        return getNormalBreakTotalSeconds(agentId, attendanceId)
                > NORMAL_BREAK_LIMIT_SECONDS;
    }

    // BREAK SUMMARY
    @Transactional(readOnly = true)
    public BreakSummary getBreakSummary(Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID is required.");
        }

        LocalDate today = LocalDate.now(INDIA_ZONE);

        AgentAttendance attendance =
                attendanceRepository
                        .findFirstByAgentIdAndAttendanceDateOrderByLoginTimeDesc(
                                agentId,
                                today
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No attendance record found for today."
                                )
                        );

        long normalUsedSeconds =
                getNormalBreakTotalSeconds(agentId, attendance.getId());

        long exceptionUsedSeconds =
                getExceptionBreakTotalSeconds(agentId, attendance.getId());

        AgentBreak activeBreak = getActiveBreak(agentId);

        // Only show a break belonging to this attendance session.
        if (activeBreak != null
                && !attendance.getId().equals(activeBreak.getAttendanceId())) {
            activeBreak = null;
        }

        long normalRemainingSeconds =
                Math.max(
                        0,
                        NORMAL_BREAK_LIMIT_SECONDS - normalUsedSeconds
                );

        String activeBreakStartTime = null;
        String activeBreakType = null;
        long activeBreakElapsedSeconds = 0;

        if (activeBreak != null) {
            activeBreakStartTime =
                    activeBreak.getStartTime().toString();

            activeBreakType =
                    activeBreak.getBreakType().name();

            activeBreakElapsedSeconds =
                    Math.max(
                            0,
                            Duration.between(
                                    activeBreak.getStartTime(),
                                    LocalDateTime.now(INDIA_ZONE)
                            ).getSeconds()
                    );
        }

        return new BreakSummary(
                normalUsedSeconds,
                normalRemainingSeconds,
                exceptionUsedSeconds,
                activeBreakStartTime,
                activeBreakType,
                activeBreakElapsedSeconds
        );
    }

    // BREAK SUMMARY RESPONSE
    public static class BreakSummary {

        private final long normalUsedSeconds;
        private final long normalRemainingSeconds;
        private final long exceptionUsedSeconds;
        private final String activeBreakStartTime;
        private final String activeBreakType;
        private final long activeBreakElapsedSeconds;

        public BreakSummary(
                long normalUsedSeconds,
                long normalRemainingSeconds,
                long exceptionUsedSeconds,
                String activeBreakStartTime,
                String activeBreakType,
                long activeBreakElapsedSeconds) {

            this.normalUsedSeconds = normalUsedSeconds;
            this.normalRemainingSeconds = normalRemainingSeconds;
            this.exceptionUsedSeconds = exceptionUsedSeconds;
            this.activeBreakStartTime = activeBreakStartTime;
            this.activeBreakType = activeBreakType;
            this.activeBreakElapsedSeconds = activeBreakElapsedSeconds;
        }

        public long getNormalUsedSeconds() {
            return normalUsedSeconds;
        }

        public long getNormalRemainingSeconds() {
            return normalRemainingSeconds;
        }

        public long getExceptionUsedSeconds() {
            return exceptionUsedSeconds;
        }

        public String getActiveBreakStartTime() {
            return activeBreakStartTime;
        }

        public String getActiveBreakType() {
            return activeBreakType;
        }

        public long getActiveBreakElapsedSeconds() {
            return activeBreakElapsedSeconds;
        }
    }
}