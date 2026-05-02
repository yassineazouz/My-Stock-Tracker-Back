package com.yassine.portfolio_tracker.config;

import com.yassine.portfolio_tracker.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private final AuthService authService;

    public DataInitializer(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        log.info("Ensuring demo login user exists");
        authService.createDemoUserIfMissing("yassine", "Yassine", "yassine@example.com", "password123");
    }
}
