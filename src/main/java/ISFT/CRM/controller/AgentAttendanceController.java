package ISFT.CRM.controller;

import ISFT.CRM.entity.AgentAttendance;
import ISFT.CRM.service.AgentAttendanceService;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/attendance")
public class AgentAttendanceController {

    private final AgentAttendanceService attendanceService;

    public AgentAttendanceController(
            AgentAttendanceService attendanceService) {

        this.attendanceService = attendanceService;
    }

    @GetMapping("/today")
    public Optional<AgentAttendance> getTodayAttendance(
            @RequestParam Long agentId) {

        return attendanceService.getTodayAttendance(agentId);
    }
}