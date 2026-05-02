package com.yassine.portfolio_tracker.service;

import com.yassine.portfolio_tracker.dto.AuthRequest;
import com.yassine.portfolio_tracker.dto.AuthResponse;
import com.yassine.portfolio_tracker.dto.SignupRequest;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.UserAccount;
import com.yassine.portfolio_tracker.repository.PortfolioRepository;
import com.yassine.portfolio_tracker.repository.UserAccountRepository;
import com.yassine.portfolio_tracker.security.JwtService;
import com.yassine.portfolio_tracker.security.LoginRateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private static final double STARTING_WALLET = 10_000.0;

    private final UserAccountRepository userAccountRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PortfolioRepository portfolioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginRateLimiter loginRateLimiter
    ) {
        this.userAccountRepository = userAccountRepository;
        this.portfolioRepository = portfolioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String username = normalize(request.getUsername());
        String email = normalize(request.getEmail());

        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userAccountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        UserAccount user = userAccountRepository.save(UserAccount.builder()
                .username(username)
                .email(email)
                .displayName(request.getDisplayName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build());

        createPortfolioIfMissing(username);
        return toResponse(user);
    }

    public AuthResponse login(AuthRequest request) {
        String key = normalize(request.getUsername());

        if (loginRateLimiter.isBlocked(key)) {
            throw new IllegalArgumentException("Too many failed login attempts. Please try again later.");
        }

        Optional<UserAccount> userOpt = userAccountRepository.findByUsername(key);
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            loginRateLimiter.recordFailure(key);
            throw new IllegalArgumentException("Invalid username or password.");
        }

        loginRateLimiter.reset(key);
        return toResponse(userOpt.get());
    }

    @Transactional
    public void createDemoUserIfMissing(String username, String displayName, String email, String password) {
        String normalizedUsername = normalize(username);
        if (userAccountRepository.existsByUsername(normalizedUsername)) {
            createPortfolioIfMissing(normalizedUsername);
            return;
        }

        userAccountRepository.save(UserAccount.builder()
                .username(normalizedUsername)
                .displayName(displayName)
                .email(normalize(email))
                .passwordHash(passwordEncoder.encode(password))
                .build());
        createPortfolioIfMissing(normalizedUsername);
    }

    private void createPortfolioIfMissing(String username) {
        portfolioRepository.findByOwner(username).orElseGet(() ->
                portfolioRepository.save(Portfolio.builder()
                        .owner(username)
                        .walletValue(STARTING_WALLET)
                        .initialInvestment(STARTING_WALLET)
                        .totalValue(0)
                        .activeAlerts(0)
                        .build())
        );
    }

    private AuthResponse toResponse(UserAccount user) {
        return AuthResponse.builder()
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .token(jwtService.generateToken(user.getUsername()))
                .build();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
