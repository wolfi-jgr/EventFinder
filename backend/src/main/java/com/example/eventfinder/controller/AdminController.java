package com.example.eventfinder.controller;

import com.example.eventfinder.model.User;
import com.example.eventfinder.repository.UserRepository;
import com.example.eventfinder.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminController(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        }

        User user = userOpt.get();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean ok = encoder.matches(password, user.getPasswordHash());
        if (!ok) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getUsername());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "message", "ok"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT logout is handled by client removing token from localStorage
        return ResponseEntity.ok(Map.of("message", "logged out"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        boolean isAdmin = jwtTokenProvider.validateHeader(authHeader);
        return ResponseEntity.ok(Map.of("admin", isAdmin));
    }
}
