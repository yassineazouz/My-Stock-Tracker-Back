package com.yassine.portfolio_tracker.config;

import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.repository.PortfolioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final PortfolioRepository portfolioRepository;

    public DataInitializer(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public void run(String... args) {
        portfolioRepository.findByOwner("yassine").orElseGet(() -> {
            Portfolio portfolio = Portfolio.builder()
                    .owner("yassine")
                    .walletValue(10_000.0)
                    .initialInvestment(10_000.0)
                    .activeAlerts(0)
                    .build();

            log.info("Creating default portfolio for local user '{}'", portfolio.getOwner());
            return portfolioRepository.save(portfolio);
        });
    }
}
