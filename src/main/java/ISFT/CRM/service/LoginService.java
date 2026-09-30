package ISFT.CRM.service;


import ISFT.CRM.dto.LoginRequest;
import ISFT.CRM.dto.LoginResponse;
import ISFT.CRM.entity.User;
import ISFT.CRM.exception.ResourceNotFoundException;
import ISFT.CRM.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ISFT.CRM.exception.InvalidCredentialsException;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password"));

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new InvalidCredentialsException(
                    "Invalid email or password");
        }

        if (!passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException(
                    "Invalid email or password");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                token
        );
    }
}