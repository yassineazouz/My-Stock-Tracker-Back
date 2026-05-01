package com.yassine.portfolio_tracker.service;

import com.yassine.portfolio_tracker.dto.PriceAlertRequest;
import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.AlertDirection;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.PriceAlert;
import com.yassine.portfolio_tracker.repository.PortfolioRepository;
import com.yassine.portfolio_tracker.repository.PriceAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockService stockService;

    public PriceAlertService(
            PriceAlertRepository priceAlertRepository,
            PortfolioRepository portfolioRepository,
            StockService stockService
    ) {
        this.priceAlertRepository = priceAlertRepository;
        this.portfolioRepository = portfolioRepository;
        this.stockService = stockService;
    }

    public List<PriceAlert> getAlerts(String username) {
        return priceAlertRepository.findByPortfolioOwnerOrderByCreatedAtDesc(username);
    }

    @Transactional
    public List<PriceAlert> checkActiveAlerts() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findByActiveTrueOrderByCreatedAtAsc();
        List<PriceAlert> triggeredAlerts = new ArrayList<>();

        for (PriceAlert alert : activeAlerts) {
            stockService.getQuote(alert.getSymbol()).ifPresentOrElse(
                    quote -> evaluateAlert(alert, quote, triggeredAlerts),
                    () -> {
                        alert.setLastCheckedAt(Instant.now());
                        log.warn("Skipping alert {} for {} because no quote is available", alert.getId(), alert.getSymbol());
                    }
            );
        }

        priceAlertRepository.saveAll(activeAlerts);
        triggeredAlerts.stream()
                .map(PriceAlert::getPortfolio)
                .distinct()
                .forEach(this::syncActiveAlertCount);

        return triggeredAlerts;
    }

    @Transactional
    public PriceAlert createAlert(String username, PriceAlertRequest request) {
        validate(request);

        Portfolio portfolio = portfolioRepository.findByOwner(username)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found for user: " + username));

        String symbol = request.getSymbol().trim().toUpperCase();
        StockQuote quote = stockService.getQuote(symbol).orElse(null);
        String companyName = request.getCompanyName();

        PriceAlert alert = PriceAlert.builder()
                .portfolio(portfolio)
                .symbol(symbol)
                .companyName(resolveCompanyName(companyName, quote, symbol))
                .targetPrice(request.getTargetPrice())
                .currentPrice(quote == null ? 0 : quote.getClose())
                .direction(request.getDirection())
                .active(true)
                .build();

        PriceAlert saved = priceAlertRepository.save(alert);
        syncActiveAlertCount(portfolio);
        return saved;
    }

    @Transactional
    public void deleteAlert(String username, Long alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        Portfolio portfolio = alert.getPortfolio();
        if (portfolio == null || !portfolio.getOwner().equals(username)) {
            throw new IllegalArgumentException("Alert does not belong to user: " + username);
        }

        priceAlertRepository.delete(alert);
        syncActiveAlertCount(portfolio);
    }

    private void validate(PriceAlertRequest request) {
        if (request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is required.");
        }
        if (request.getTargetPrice() <= 0) {
            throw new IllegalArgumentException("Target price must be greater than zero.");
        }
        if (request.getDirection() == null) {
            throw new IllegalArgumentException("Alert direction is required.");
        }
    }

    private String resolveCompanyName(String companyName, StockQuote quote, String symbol) {
        if (companyName != null && !companyName.trim().isEmpty()) {
            return companyName.trim();
        }
        if (quote != null && quote.getName() != null && !quote.getName().isBlank()) {
            return quote.getName();
        }
        return symbol;
    }

    private void syncActiveAlertCount(Portfolio portfolio) {
        portfolio.setActiveAlerts((int) priceAlertRepository.countByPortfolioOwnerAndActiveTrue(portfolio.getOwner()));
        portfolioRepository.save(portfolio);
    }

    private void evaluateAlert(PriceAlert alert, StockQuote quote, List<PriceAlert> triggeredAlerts) {
        Instant checkedAt = Instant.now();
        double currentPrice = quote.getClose();
        boolean isTriggered = isTriggered(alert, currentPrice);

        alert.setCurrentPrice(currentPrice);
        alert.setLastCheckedAt(checkedAt);

        if (!isTriggered) {
            return;
        }

        alert.setActive(false);
        alert.setTriggeredAt(checkedAt);
        alert.setTriggeredPrice(currentPrice);
        triggeredAlerts.add(alert);

        log.info(
                "Triggered {} alert {} for {} at {} target {}",
                alert.getDirection(),
                alert.getId(),
                alert.getSymbol(),
                currentPrice,
                alert.getTargetPrice()
        );
    }

    private boolean isTriggered(PriceAlert alert, double currentPrice) {
        if (alert.getDirection() == AlertDirection.ABOVE) {
            return currentPrice >= alert.getTargetPrice();
        }

        return currentPrice <= alert.getTargetPrice();
    }
}
