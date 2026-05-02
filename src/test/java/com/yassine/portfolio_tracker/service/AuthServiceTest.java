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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserAccountRepository userAccountRepository;
    @Mock PortfolioRepository portfolioRepository;
    @Mock JwtService jwtService;
    @Mock LoginRateLimiter loginRateLimiter;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userAccountRepository, portfolioRepository, passwordEncoder, jwtService, loginRateLimiter);
    }

    @Test
    void signup_createsUserAndPortfolio() {
        SignupRequest request = new SignupRequest();
        request.setUsername(" New_User ");
        request.setDisplayName("New User");
        request.setEmail(" New@Example.com ");
        request.setPassword("password123");

        when(userAccountRepository.existsByUsername("new_user")).thenReturn(false);
        when(userAccountRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(portfolioRepository.findByOwner("new_user")).thenReturn(Optional.empty());
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("new_user")).thenReturn("signup-token");

        AuthResponse response = authService.signup(request);

        assertThat(response.getUsername()).isEqualTo("new_user");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getToken()).isEqualTo("signup-token");
        verify(portfolioRepository).save(argThat(portfolio ->
                portfolio.getOwner().equals("new_user") && portfolio.getWalletValue().equals(10_000.0)
        ));
    }

    @Test
    void signup_rejectsDuplicateUsername() {
        SignupRequest request = new SignupRequest();
        request.setUsername("yassine");
        request.setDisplayName("Yassine");
        request.setEmail("yassine@example.com");
        request.setPassword("password123");

        when(userAccountRepository.existsByUsername("yassine")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    void login_acceptsValidPassword() {
        UserAccount user = UserAccount.builder()
                .username("yassine")
                .displayName("Yassine")
                .email("yassine@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();

        AuthRequest request = new AuthRequest();
        request.setUsername("YASSINE");
        request.setPassword("password123");

        when(userAccountRepository.findByUsername("yassine")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("yassine")).thenReturn("login-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getUsername()).isEqualTo("yassine");
        assertThat(response.getDisplayName()).isEqualTo("Yassine");
        assertThat(response.getToken()).isEqualTo("login-token");
    }

    @Test
    void login_rejectsInvalidPassword() {
        UserAccount user = UserAccount.builder()
                .username("yassine")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();

        AuthRequest request = new AuthRequest();
        request.setUsername("yassine");
        request.setPassword("wrong-password");

        when(userAccountRepository.findByUsername("yassine")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
