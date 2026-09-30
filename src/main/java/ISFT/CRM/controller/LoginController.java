package ISFT.CRM.controller;


import ISFT.CRM.dto.LoginRequest;
import ISFT.CRM.dto.LoginResponse;
import ISFT.CRM.service.LoginService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest loginRequest) {

        return loginService.login(loginRequest);
    }
}