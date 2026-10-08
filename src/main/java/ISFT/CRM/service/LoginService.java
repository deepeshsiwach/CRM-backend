package ISFT.CRM.service;

import ISFT.CRM.dto.LoginRequest;
import ISFT.CRM.dto.LoginResponse;
import ISFT.CRM.entity.User;
import ISFT.CRM.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ISFT.CRM.exception.InvalidCredentialsException;

@Service
public class LoginService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AgentAttendanceService agentAttendanceService;


    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AgentAttendanceService agentAttendanceService) {

        this.userRepository = userRepository;

        this.passwordEncoder = passwordEncoder;

        this.jwtService = jwtService;

        this.agentAttendanceService =
                agentAttendanceService;
    }


    // ========================================
    // LOGIN
    // ========================================

    public LoginResponse login(
            LoginRequest loginRequest) {


        // ====================================
        // FIND USER
        // ====================================

        User user =
                userRepository
                        .findByEmail(
                                loginRequest.getEmail()
                        )
                        .orElseThrow(
                                () ->
                                        new InvalidCredentialsException(
                                                "Invalid email or password"
                                        )
                        );


        // ====================================
        // CHECK USER STATUS
        // ====================================

        if (user.getStatus() != User.Status.ACTIVE) {

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }


        // ====================================
        // CHECK PASSWORD
        // ====================================

        if (!passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }


        // ====================================
        // GENERATE JWT
        // ====================================

        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole().name()
                );


        // ====================================
        // RECORD AGENT LOGIN
        // ====================================

        if (user.getRole() == User.Role.AGENT) {

            agentAttendanceService.recordLogin(
                    user.getId()
            );
        }


        // ====================================
        // RETURN LOGIN RESPONSE
        // ====================================

        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                token
        );
    }
}