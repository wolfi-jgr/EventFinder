package com.example.eventfinder.controller;

import com.example.eventfinder.model.User;
import com.example.eventfinder.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
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

        HttpSession session = request.getSession(true);
        session.setAttribute("isAdmin", true);
        session.setAttribute("adminUsername", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "logged out"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean isAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        return ResponseEntity.ok(Map.of("admin", isAdmin));
    }
}
