
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

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final long LUNCH_LIMIT_SECONDS = 35 * 60L;
    private static final long TEA_LIMIT_SECONDS = 30 * 60L;
    private static final long BIO_LIMIT_SECONDS = 20 * 60L;

    private static final long TOTAL_NORMAL_LIMIT_SECONDS =
            LUNCH_LIMIT_SECONDS
                    + TEA_LIMIT_SECONDS
                    + BIO_LIMIT_SECONDS;

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
            throw new IllegalArgumentException(
                    "Agent ID is required.");
        }

        if (breakType == null) {
            throw new IllegalArgumentException(
                    "Break type is required.");
        }

        if (breakType == AgentBreak.BreakType.EXCEPTION
                && (reason == null || reason.trim().isEmpty())) {

            throw new IllegalArgumentException(
                    "Reason is required for an exception break.");
        }

        if (breakType == AgentBreak.BreakType.NORMAL) {
            throw new IllegalArgumentException(
                    "Select Lunch, Tea, Bio, or Exception Break.");
        }

        AgentAttendance attendance =
                attendanceRepository
                        .findFirstByAgentIdAndLogoutTimeIsNullOrderByLoginTimeDesc(
                                agentId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No active attendance session. Please log in first."));

        AgentBreak activeBreak =
                breakRepository
                        .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                                agentId)
                        .orElse(null);

        if (activeBreak != null) {

            if (!activeBreak.getAttendanceId()
                    .equals(attendance.getId())) {

                closeBreak(
                        activeBreak,
                        LocalDateTime.now(INDIA_ZONE));

                breakRepository.save(activeBreak);

            } else {
                throw new IllegalStateException(
                        "You already have an active break.");
            }
        }

        if (breakType != AgentBreak.BreakType.EXCEPTION) {

            long usedSeconds =
                    getBreakTypeUsedSeconds(
                            agentId,
                            attendance.getId(),
                            breakType);

            long limitSeconds = getLimitSeconds(breakType);

            if (usedSeconds >= limitSeconds) {
                throw new IllegalStateException(
                        getBreakLabel(breakType)
                                + " allowance is exhausted for today.");
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
                        : reason.trim());

        agentBreak.setDurationSeconds(null);
        agentBreak.setLimitAlertSent(false);

        return breakRepository.save(agentBreak);
    }

    // END BREAK
    @Transactional
    public AgentBreak endBreak(Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException(
                    "Agent ID is required.");
        }

        AgentBreak agentBreak =
                breakRepository
                        .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                                agentId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No active break found."));

        closeBreak(agentBreak, LocalDateTime.now(INDIA_ZONE));

        return breakRepository.save(agentBreak);
    }

    // CLOSE ACTIVE BREAK WHEN ATTENDANCE ENDS
    @Transactional
    public void closeActiveBreakForAttendance(
            Long agentId,
            Long attendanceId,
            LocalDateTime endTime) {

        if (agentId == null
                || attendanceId == null
                || endTime == null) {
            return;
        }

        AgentBreak activeBreak =
                breakRepository
                        .findFirstByAgentIdAndEndTimeIsNullOrderByStartTimeDesc(
                                agentId)
                        .orElse(null);

        if (activeBreak != null
                && attendanceId.equals(
                activeBreak.getAttendanceId())) {

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
                                        effectiveEnd)
                                .getSeconds());

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
                        agentId)
                .orElse(null);
    }

    // TOTAL NORMAL BREAK: LUNCH + TEA + BIO + LEGACY NORMAL
    @Transactional(readOnly = true)
    public long getNormalBreakTotalSeconds(
            Long agentId,
            Long attendanceId) {

        return getBreaks(agentId, attendanceId).stream()
                .filter(b ->
                        b.getBreakType() == AgentBreak.BreakType.NORMAL
                                || b.getBreakType() == AgentBreak.BreakType.LUNCH
                                || b.getBreakType() == AgentBreak.BreakType.TEA
                                || b.getBreakType() == AgentBreak.BreakType.BIO)
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
                        b.getBreakType()
                                == AgentBreak.BreakType.EXCEPTION)
                .mapToLong(this::getEffectiveDuration)
                .sum();
    }

    // USED TIME FOR ONE BREAK TYPE
    @Transactional(readOnly = true)
    public long getBreakTypeUsedSeconds(
            Long agentId,
            Long attendanceId,
            AgentBreak.BreakType breakType) {

        if (breakType == null
                || breakType == AgentBreak.BreakType.NORMAL
                || breakType == AgentBreak.BreakType.EXCEPTION) {
            return 0;
        }

        return getBreaks(agentId, attendanceId).stream()
                .filter(b -> b.getBreakType() == breakType)
                .mapToLong(this::getEffectiveDuration)
                .sum();
    }

    private List<AgentBreak> getBreaks(
            Long agentId,
            Long attendanceId) {

        return breakRepository
                .findByAgentIdAndAttendanceIdOrderByStartTimeAsc(
                        agentId,
                        attendanceId);
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
                                    LocalDateTime.now(INDIA_ZONE))
                            .getSeconds());
        }

        return Math.max(
                0,
                Duration.between(
                                agentBreak.getStartTime(),
                                agentBreak.getEndTime())
                        .getSeconds());
    }

    private long getLimitSeconds(
            AgentBreak.BreakType breakType) {

        switch (breakType) {
            case LUNCH:
                return LUNCH_LIMIT_SECONDS;

            case TEA:
                return TEA_LIMIT_SECONDS;

            case BIO:
                return BIO_LIMIT_SECONDS;

            default:
                return 0;
        }
    }

    private String getBreakLabel(
            AgentBreak.BreakType breakType) {

        switch (breakType) {
            case LUNCH:
                return "Lunch Break";

            case TEA:
                return "Tea Break";

            case BIO:
                return "Bio Break";

            default:
                return "Normal Break";
        }
    }

    // CHECK OVERALL NORMAL BREAK LIMIT
    @Transactional(readOnly = true)
    public boolean isNormalBreakOverLimit(
            Long agentId,
            Long attendanceId) {

        return getNormalBreakTotalSeconds(
                agentId,
                attendanceId) > TOTAL_NORMAL_LIMIT_SECONDS;
    }

    // BREAK SUMMARY
    @Transactional(readOnly = true)
    public BreakSummary getBreakSummary(Long agentId) {

        if (agentId == null) {
            throw new IllegalArgumentException(
                    "Agent ID is required.");
        }

        LocalDate today = LocalDate.now(INDIA_ZONE);

        AgentAttendance attendance =
                attendanceRepository
                        .findFirstByAgentIdAndAttendanceDateOrderByLoginTimeDesc(
                                agentId,
                                today)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No attendance record found for today."));

        Long attendanceId = attendance.getId();

        long lunchUsed = getBreakTypeUsedSeconds(
                agentId, attendanceId, AgentBreak.BreakType.LUNCH);

        long teaUsed = getBreakTypeUsedSeconds(
                agentId, attendanceId, AgentBreak.BreakType.TEA);

        long bioUsed = getBreakTypeUsedSeconds(
                agentId, attendanceId, AgentBreak.BreakType.BIO);

        long legacyNormalUsed = getBreaks(
                agentId, attendanceId).stream()
                .filter(b ->
                        b.getBreakType() == AgentBreak.BreakType.NORMAL)
                .mapToLong(this::getEffectiveDuration)
                .sum();

        long normalUsedSeconds =
                lunchUsed + teaUsed + bioUsed + legacyNormalUsed;

        long exceptionUsedSeconds =
                getExceptionBreakTotalSeconds(agentId, attendanceId);

        AgentBreak activeBreak = getActiveBreak(agentId);

        if (activeBreak != null
                && !attendanceId.equals(activeBreak.getAttendanceId())) {
            activeBreak = null;
        }

        long normalRemainingSeconds =
                Math.max(
                        0,
                        TOTAL_NORMAL_LIMIT_SECONDS - normalUsedSeconds);

        String activeBreakStartTime = null;
        String activeBreakType = null;
        long activeBreakElapsedSeconds = 0;

        if (activeBreak != null) {

            activeBreakStartTime =
                    activeBreak.getStartTime().toString();

            activeBreakType =
                    activeBreak.getBreakType().name();

            activeBreakElapsedSeconds =
                    getEffectiveDuration(activeBreak);
        }

        return new BreakSummary(
                normalUsedSeconds,
                normalRemainingSeconds,
                exceptionUsedSeconds,
                activeBreakStartTime,
                activeBreakType,
                activeBreakElapsedSeconds,
                lunchUsed,
                Math.max(0, LUNCH_LIMIT_SECONDS - lunchUsed),
                teaUsed,
                Math.max(0, TEA_LIMIT_SECONDS - teaUsed),
                bioUsed,
                Math.max(0, BIO_LIMIT_SECONDS - bioUsed)
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

        private final long lunchUsedSeconds;
        private final long lunchRemainingSeconds;
        private final long teaUsedSeconds;
        private final long teaRemainingSeconds;
        private final long bioUsedSeconds;
        private final long bioRemainingSeconds;

        public BreakSummary(
                long normalUsedSeconds,
                long normalRemainingSeconds,
                long exceptionUsedSeconds,
                String activeBreakStartTime,
                String activeBreakType,
                long activeBreakElapsedSeconds,
                long lunchUsedSeconds,
                long lunchRemainingSeconds,
                long teaUsedSeconds,
                long teaRemainingSeconds,
                long bioUsedSeconds,
                long bioRemainingSeconds) {

            this.normalUsedSeconds = normalUsedSeconds;
            this.normalRemainingSeconds = normalRemainingSeconds;
            this.exceptionUsedSeconds = exceptionUsedSeconds;
            this.activeBreakStartTime = activeBreakStartTime;
            this.activeBreakType = activeBreakType;
            this.activeBreakElapsedSeconds = activeBreakElapsedSeconds;

            this.lunchUsedSeconds = lunchUsedSeconds;
            this.lunchRemainingSeconds = lunchRemainingSeconds;
            this.teaUsedSeconds = teaUsedSeconds;
            this.teaRemainingSeconds = teaRemainingSeconds;
            this.bioUsedSeconds = bioUsedSeconds;
            this.bioRemainingSeconds = bioRemainingSeconds;
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

        public long getLunchUsedSeconds() {
            return lunchUsedSeconds;
        }

        public long getLunchRemainingSeconds() {
            return lunchRemainingSeconds;
        }

        public long getTeaUsedSeconds() {
            return teaUsedSeconds;
        }

        public long getTeaRemainingSeconds() {
            return teaRemainingSeconds;
        }

        public long getBioUsedSeconds() {
            return bioUsedSeconds;
        }

        public long getBioRemainingSeconds() {
            return bioRemainingSeconds;
        }
    }
}
