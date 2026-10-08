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
            @RequestParam Long agentId,
            @RequestParam AgentBreak.BreakType breakType,
            @RequestParam(required = false) String reason) {

        return agentBreakService.startBreak(
                agentId,
                breakType,
                reason
        );
    }

    @PostMapping("/end")
    public AgentBreak endBreak(
            @RequestParam Long agentId) {

        return agentBreakService.endBreak(agentId);
    }

    @GetMapping("/active")
    public AgentBreak getActiveBreak(
            @RequestParam Long agentId) {

        return agentBreakService.getActiveBreak(agentId);
    }
}