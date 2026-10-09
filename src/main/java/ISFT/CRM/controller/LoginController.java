package ISFT.CRM.controller;

import ISFT.CRM.dto.LoginRequest;
import ISFT.CRM.dto.LoginResponse;
import ISFT.CRM.service.LoginService;
import ISFT.CRM.service.AgentAttendanceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final LoginService loginService;
    private final AgentAttendanceService agentAttendanceService;

    public LoginController(
            LoginService loginService,
            AgentAttendanceService agentAttendanceService) {
        this.loginService = loginService;
        this.agentAttendanceService = agentAttendanceService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest loginRequest) {
        return loginService.login(loginRequest);
    }

    @PostMapping("/logout/{agentId}")
    public String logout(@PathVariable Long agentId) {
        agentAttendanceService.recordLogout(agentId);
        return "Logout attendance recorded successfully.";
    }
}