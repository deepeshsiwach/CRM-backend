
package ISFT.CRM.controller;

import ISFT.CRM.entity.AgentBreak;
import ISFT.CRM.service.AgentBreakService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance/break")
public class AgentBreakController {

    private final AgentBreakService agentBreakService;

    public AgentBreakController(
            AgentBreakService agentBreakService) {
        this.agentBreakService = agentBreakService;
    }

    // START BREAK
    @PostMapping("/start")
    public AgentBreak startBreak(
            @RequestBody StartBreakRequest request) {

        if (request == null || request.getAgentId() == null) {
            throw new IllegalArgumentException(
                    "Agent ID is required.");
        }

        if (request.getBreakType() == null) {
            throw new IllegalArgumentException(
                    "Select a break type.");
        }

        return agentBreakService.startBreak(
                request.getAgentId(),
                request.getBreakType(),
                request.getReason()
        );
    }

    // END BREAK
    @PostMapping("/end")
    public AgentBreak endBreak(
            @RequestBody EndBreakRequest request) {

        if (request == null || request.getAgentId() == null) {
            throw new IllegalArgumentException(
                    "Agent ID is required.");
        }

        return agentBreakService.endBreak(
                request.getAgentId());
    }

    // GET ACTIVE BREAK
    @GetMapping("/active")
    public AgentBreak getActiveBreak(
            @RequestParam Long agentId) {

        return agentBreakService.getActiveBreak(agentId);
    }

    // GET TODAY'S BREAK SUMMARY
    @GetMapping("/summary")
    public AgentBreakService.BreakSummary getBreakSummary(
            @RequestParam Long agentId) {

        return agentBreakService.getBreakSummary(agentId);
    }

    // HANDLE VALIDATION ERRORS
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException exception) {

        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // HANDLE BREAK RULE ERRORS
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            IllegalStateException exception) {

        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    public static class StartBreakRequest {

        private Long agentId;
        private AgentBreak.BreakType breakType;
        private String reason;

        public Long getAgentId() {
            return agentId;
        }

        public void setAgentId(Long agentId) {
            this.agentId = agentId;
        }

        public AgentBreak.BreakType getBreakType() {
            return breakType;
        }

        public void setBreakType(
                AgentBreak.BreakType breakType) {
            this.breakType = breakType;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class EndBreakRequest {

        private Long agentId;

        public Long getAgentId() {
            return agentId;
        }

        public void setAgentId(Long agentId) {
            this.agentId = agentId;
        }
    }
}
