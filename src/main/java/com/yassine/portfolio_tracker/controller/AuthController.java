package com.yassine.portfolio_tracker.controller;

import com.yassine.portfolio_tracker.dto.AuthRequest;
import com.yassine.portfolio_tracker.dto.AuthResponse;
import com.yassine.portfolio_tracker.dto.SignupRequest;
import com.yassine.portfolio_tracker.security.LoginRateLimiter;
import com.yassine.portfolio_tracker.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Login and sign-up")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/signup")
    @Operation(summary = "Create a user and initial portfolio")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        String key = rateLimitKey(request, httpRequest);
        if (loginRateLimiter.isBlocked(key)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts. Try again later.");
        }

        try {
            AuthResponse response = authService.login(request);
            loginRateLimiter.reset(key);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            loginRateLimiter.recordFailure(key);
            throw ex;
        }
    }

    private String rateLimitKey(AuthRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim().toLowerCase();
        return httpRequest.getRemoteAddr() + ":" + username;
    }
}
