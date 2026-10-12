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

    // Get today's attendance
    @GetMapping("/today")
    public Optional<AgentAttendance> getTodayAttendance(
            @RequestParam Long agentId) {
        return attendanceService.getTodayAttendance(agentId);
    }

    // Record agent login
    @PostMapping("/login")
    public AgentAttendance login(
            @RequestParam Long agentId) {
        return attendanceService.recordLogin(agentId);
    }

    // Record agent logout
    @PostMapping("/logout")
    public AgentAttendance logout(
            @RequestParam Long agentId) {
        return attendanceService.recordLogout(agentId);
    }
}