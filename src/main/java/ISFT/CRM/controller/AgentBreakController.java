package ISFT.CRM.controller;

import ISFT.CRM.entity.AgentBreak;
import ISFT.CRM.service.AgentBreakService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance/break")
public class AgentBreakController {

    private final AgentBreakService agentBreakService;

    public AgentBreakController(
            AgentBreakService agentBreakService) {

        this.agentBreakService = agentBreakService;
    }

    @PostMapping("/start")
    public AgentBreak startBreak(
            @RequestBody StartBreakRequest request) {

        return agentBreakService.startBreak(
                request.getAgentId(),
                request.getBreakType(),
                request.getReason()
        );
    }

    @PostMapping("/end")
    public AgentBreak endBreak(
            @RequestBody EndBreakRequest request) {

        return agentBreakService.endBreak(
                request.getAgentId()
        );
    }

    @GetMapping("/active")
    public AgentBreak getActiveBreak(
            @RequestParam Long agentId) {

        return agentBreakService.getActiveBreak(agentId);
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