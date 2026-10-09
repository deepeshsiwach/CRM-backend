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


    // ============================================================
    // START BREAK
    // ============================================================

    @Transactional
    public AgentBreak startBreak(
            Long agentId,
            AgentBreak.BreakType breakType,
            String reason) {

        AgentBreak activeBreak =
                breakRepository
                        .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                                agentId
                        )
                        .orElse(null);

        if (activeBreak != null) {
            throw new IllegalStateException(
                    "You already have an active break."
            );
        }

        AgentAttendance attendance =
                attendanceRepository
                        .findByAgentIdAndAttendanceDate(
                                agentId,
                                LocalDate.now(INDIA_ZONE)
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No attendance record found for today."
                                )
                        );

        if (attendance.getLogoutTime() != null) {
            throw new IllegalStateException(
                    "You have already logged out today."
            );
        }

        if (breakType == null) {
            throw new IllegalArgumentException(
                    "Break type is required."
            );
        }

        if (breakType == AgentBreak.BreakType.EXCEPTION) {

            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Reason is required for an exception break."
                );
            }
        }

        AgentBreak agentBreak = new AgentBreak();

        agentBreak.setAgentId(agentId);
        agentBreak.setAttendanceId(attendance.getId());
        agentBreak.setBreakType(breakType);
        agentBreak.setStartTime(LocalDateTime.now(INDIA_ZONE));

        agentBreak.setReason(
                reason == null
                        ? null
                        : reason.trim()
        );

        agentBreak.setDurationSeconds(null);
        agentBreak.setLimitAlertSent(false);

        return breakRepository.save(agentBreak);
    }


    // ============================================================
    // END BREAK
    // ============================================================

    @Transactional
    public AgentBreak endBreak(Long agentId) {

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

        LocalDateTime endTime =
                LocalDateTime.now(INDIA_ZONE);

        long durationSeconds =
                Duration.between(
                        agentBreak.getStartTime(),
                        endTime
                ).getSeconds();

        agentBreak.setEndTime(endTime);
        agentBreak.setDurationSeconds(durationSeconds);

        return breakRepository.save(agentBreak);
    }


    // ============================================================
    // ACTIVE BREAK
    // ============================================================

    public AgentBreak getActiveBreak(Long agentId) {

        return breakRepository
                .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                        agentId
                )
                .orElse(null);
    }


    // ============================================================
    // NORMAL BREAK TOTAL
    // ============================================================

    public long getNormalBreakTotalSeconds(
            Long agentId,
            Long attendanceId) {

        List<AgentBreak> breaks =
                breakRepository
                        .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                                agentId,
                                attendanceId
                        );

        return breaks.stream()
                .filter(b ->
                        b.getBreakType()
                                == AgentBreak.BreakType.NORMAL
                )
                .mapToLong(this::getEffectiveDuration)
                .sum();
    }


    // ============================================================
    // EXCEPTION BREAK TOTAL
    // ============================================================

    public long getExceptionBreakTotalSeconds(
            Long agentId,
            Long attendanceId) {

        List<AgentBreak> breaks =
                breakRepository
                        .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                                agentId,
                                attendanceId
                        );

        return breaks.stream()
                .filter(b ->
                        b.getBreakType()
                                == AgentBreak.BreakType.EXCEPTION
                )
                .mapToLong(this::getEffectiveDuration)
                .sum();
    }


    // ============================================================
    // EFFECTIVE BREAK DURATION
    // ============================================================

    private long getEffectiveDuration(
            AgentBreak agentBreak) {

        if (agentBreak.getDurationSeconds() != null) {
            return agentBreak.getDurationSeconds();
        }

        if (agentBreak.getEndTime() == null) {

            return Duration.between(
                    agentBreak.getStartTime(),
                    LocalDateTime.now(INDIA_ZONE)
            ).getSeconds();
        }

        return 0;
    }


    // ============================================================
    // CHECK NORMAL BREAK LIMIT
    // ============================================================

    public boolean isNormalBreakOverLimit(
            Long agentId,
            Long attendanceId) {

        return getNormalBreakTotalSeconds(
                agentId,
                attendanceId
        ) > NORMAL_BREAK_LIMIT_SECONDS;
    }


    // ============================================================
    // BREAK SUMMARY
    // ============================================================

    public BreakSummary getBreakSummary(
            Long agentId) {

        AgentAttendance attendance =
                attendanceRepository
                        .findByAgentIdAndAttendanceDate(
                                agentId,
                                LocalDate.now(INDIA_ZONE)
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No attendance record found for today."
                                )
                        );

        long normalUsedSeconds =
                getNormalBreakTotalSeconds(
                        agentId,
                        attendance.getId()
                );

        long exceptionUsedSeconds =
                getExceptionBreakTotalSeconds(
                        agentId,
                        attendance.getId()
                );

        AgentBreak activeBreak =
                getActiveBreak(agentId);

        long normalRemainingSeconds =
                Math.max(
                        0,
                        NORMAL_BREAK_LIMIT_SECONDS
                                - normalUsedSeconds
                );

        String activeBreakStartTime = null;

        String activeBreakType = null;

        long activeBreakElapsedSeconds = 0;


        // ========================================================
        // SERVER-SIDE ACTIVE BREAK TIMER
        // ========================================================

        if (activeBreak != null) {

            activeBreakStartTime =
                    activeBreak
                            .getStartTime()
                            .toString();

            activeBreakType =
                    activeBreak
                            .getBreakType()
                            .name();

            /*
             * Calculate elapsed time on the SERVER.
             *
             * This avoids browser timezone problems.
             */
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


    // ============================================================
    // BREAK SUMMARY RESPONSE
    // ============================================================

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

            this.normalUsedSeconds =
                    normalUsedSeconds;

            this.normalRemainingSeconds =
                    normalRemainingSeconds;

            this.exceptionUsedSeconds =
                    exceptionUsedSeconds;

            this.activeBreakStartTime =
                    activeBreakStartTime;

            this.activeBreakType =
                    activeBreakType;

            this.activeBreakElapsedSeconds =
                    activeBreakElapsedSeconds;
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