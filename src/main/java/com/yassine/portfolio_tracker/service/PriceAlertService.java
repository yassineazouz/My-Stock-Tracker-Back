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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return checkActiveAlerts(priceAlertRepository.findByActiveTrueOrderByCreatedAtAsc());
    }

    @Transactional
    public List<PriceAlert> checkActiveAlerts(String username) {
        return checkActiveAlerts(priceAlertRepository.findByPortfolioOwnerAndActiveTrueOrderByCreatedAtAsc(username));
    }

    private List<PriceAlert> checkActiveAlerts(List<PriceAlert> activeAlerts) {
        if (activeAlerts.isEmpty()) {
            return List.of();
        }

        // Fetch each unique symbol once — avoids N API calls for N alerts on the same symbol
        Map<String, StockQuote> quoteBySymbol = activeAlerts.stream()
                .map(PriceAlert::getSymbol)
                .distinct()
                .flatMap(symbol -> stockService.getQuote(symbol).stream()
                        .map(quote -> Map.entry(symbol, quote)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<PriceAlert> triggeredAlerts = new ArrayList<>();

        for (PriceAlert alert : activeAlerts) {
            StockQuote quote = quoteBySymbol.get(alert.getSymbol());
            if (quote == null) {
                alert.setLastCheckedAt(Instant.now());
                log.warn("Skipping alert {} for {} — no quote available", alert.getId(), alert.getSymbol());
            } else {
                evaluateAlert(alert, quote, triggeredAlerts);
            }
        }

        priceAlertRepository.saveAll(activeAlerts);

        triggeredAlerts.stream()
                .map(PriceAlert::getPortfolio)
                .filter(Objects::nonNull)
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

        PriceAlert alert = PriceAlert.builder()
                .portfolio(portfolio)
                .symbol(symbol)
                .companyName(resolveCompanyName(request.getCompanyName(), quote, symbol))
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
        if (request.getSymbol() == null || request.getSymbol().isBlank()) {
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
        if (companyName != null && !companyName.isBlank()) {
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
        Instant now = Instant.now();
        double currentPrice = quote.getClose();

        alert.setCurrentPrice(currentPrice);
        alert.setLastCheckedAt(now);

        if (!isTriggered(alert, currentPrice)) {
            return;
        }

        alert.setActive(false);
        alert.setTriggeredAt(now);
        alert.setTriggeredPrice(currentPrice);
        triggeredAlerts.add(alert);

        log.info("Alert {} triggered: {} {} at {} (target {})",
                alert.getId(), alert.getSymbol(), alert.getDirection(),
                currentPrice, alert.getTargetPrice());
    }

    private boolean isTriggered(PriceAlert alert, double currentPrice) {
        return alert.getDirection() == AlertDirection.ABOVE
                ? currentPrice >= alert.getTargetPrice()
                : currentPrice <= alert.getTargetPrice();
    }
}
