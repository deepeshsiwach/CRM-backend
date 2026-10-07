package ISFT.CRM.service;

import ISFT.CRM.entity.User;
import ISFT.CRM.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(User user) {

        // Validate full name
        if (user.getFullName() == null ||
                user.getFullName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "User full name is required");
        }

        // Validate email presence BEFORE using matches()
        if (user.getEmail() == null ||
                user.getEmail().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "User email is required");
        }

        // Validate email format
        if (!user.getEmail().matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            throw new IllegalArgumentException(
                    "User email format is invalid");
        }

        // Check duplicate email
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {

            throw new IllegalArgumentException(
                    "User email already exists");
        }

        // Validate password presence BEFORE using length()
        if (user.getPassword() == null ||
                user.getPassword().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "User password is required");
        }

        // Validate password length
        if (user.getPassword().length() < 8) {

            throw new IllegalArgumentException(
                    "User password must be at least 8 characters");
        }

        // Validate password complexity
        if (!user.getPassword().matches(".*[A-Za-z].*") ||
                !user.getPassword().matches(".*\\d.*")) {

            throw new IllegalArgumentException(
                    "User password must contain at least one letter and one number");
        }

        // Validate phone
        if (user.getPhone() != null &&
                !user.getPhone().trim().isEmpty() &&
                !user.getPhone().matches("\\d{10}")) {

            throw new IllegalArgumentException(
                    "User phone number must contain exactly 10 digits");
        }

        // Hash password before storing
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Validate full name
        if (userDetails.getFullName() == null ||
                userDetails.getFullName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "User full name is required");
        }

        // Validate email
        if (userDetails.getEmail() == null ||
                userDetails.getEmail().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "User email is required");
        }

        if (!userDetails.getEmail().matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            throw new IllegalArgumentException(
                    "User email format is invalid");
        }

        // Check whether the email belongs to another user
        Optional<User> userWithSameEmail =
                userRepository.findByEmail(userDetails.getEmail());

        if (userWithSameEmail.isPresent() &&
                !userWithSameEmail.get().getId().equals(id)) {

            throw new IllegalArgumentException(
                    "User email already exists");
        }

        // Validate phone
        if (userDetails.getPhone() != null &&
                !userDetails.getPhone().trim().isEmpty() &&
                !userDetails.getPhone().matches("\\d{10}")) {

            throw new IllegalArgumentException(
                    "User phone number must contain exactly 10 digits");
        }

        // Update basic user information
        existingUser.setFullName(userDetails.getFullName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());
        existingUser.setRole(userDetails.getRole());
        existingUser.setStatus(userDetails.getStatus());

        // Only change password when a new password is actually provided
        if (userDetails.getPassword() != null &&
                !userDetails.getPassword().trim().isEmpty()) {

            if (userDetails.getPassword().length() < 8) {

                throw new IllegalArgumentException(
                        "User password must be at least 8 characters");
            }

            if (!userDetails.getPassword().matches(".*[A-Za-z].*") ||
                    !userDetails.getPassword().matches(".*\\d.*")) {

                throw new IllegalArgumentException(
                        "User password must contain at least one letter and one number");
            }

            existingUser.setPassword(
                    passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        userRepository.delete(existingUser);
    }
}

