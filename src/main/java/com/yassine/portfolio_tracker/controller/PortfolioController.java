package com.yassine.portfolio_tracker.controller;

import com.yassine.portfolio_tracker.dto.BuyStockRequest;
import com.yassine.portfolio_tracker.dto.PriceAlertRequest;
import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.PriceAlert;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.service.PortfolioService;
import com.yassine.portfolio_tracker.service.PriceAlertService;
import com.yassine.portfolio_tracker.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Portfolio", description = "Portfolio management, market data, and price alerts")
public class PortfolioController {

    private final StockService stockService;
    private final PortfolioService portfolioService;
    private final PriceAlertService priceAlertService;

    public PortfolioController(StockService stockService,
                               PortfolioService portfolioService,
                               PriceAlertService priceAlertService) {
        this.stockService = stockService;
        this.portfolioService = portfolioService;
        this.priceAlertService = priceAlertService;
    }

    // ── Portfolio ────────────────────────────────────────────────────────────

    @GetMapping("/user/{username}")
    @Operation(summary = "Get portfolio by username")
    public ResponseEntity<Portfolio> getPortfolio(@PathVariable String username, Authentication authentication) {
        requireSameUser(username, authentication);
        return portfolioService.getPortfolioByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/user/stock/{username}")
    @Operation(summary = "Buy a stock and deduct cost from wallet")
    public ResponseEntity<Stock> buyStock(@PathVariable String username,
                                          @Valid @RequestBody BuyStockRequest request,
                                          Authentication authentication) {
        requireSameUser(username, authentication);
        return ResponseEntity.ok(portfolioService.buyStock(request, username));
    }

    @DeleteMapping("/user/{username}/stocks/{stockId}")
    @Operation(summary = "Sell a stock and credit proceeds to wallet")
    public ResponseEntity<Void> sellStock(@PathVariable String username,
                                          @PathVariable Long stockId,
                                          Authentication authentication) {
        requireSameUser(username, authentication);
        portfolioService.sellStock(stockId, username);
        return ResponseEntity.noContent().build();
    }

    // ── Market data ──────────────────────────────────────────────────────────

    @GetMapping("/stocks/all")
    @Operation(summary = "Get latest quotes for tracked symbols")
    public List<StockQuote> getStockList() {
        return stockService.getTopStockPrices();
    }

    @GetMapping("/stocks/top-performer")
    @Operation(summary = "Get the top-performing symbol from the last fetch")
    public ResponseEntity<StockQuote> getTopPerformer() {
        return stockService.getTopPerformer()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Price alerts ─────────────────────────────────────────────────────────

    @GetMapping("/alerts/{username}")
    @Operation(summary = "List all alerts for a user")
    public List<PriceAlert> getAlerts(@PathVariable String username, Authentication authentication) {
        requireSameUser(username, authentication);
        return priceAlertService.getAlerts(username);
    }

    @PostMapping("/alerts/{username}")
    @Operation(summary = "Create a new price alert")
    public ResponseEntity<PriceAlert> createAlert(@PathVariable String username,
                                                   @Valid @RequestBody PriceAlertRequest request,
                                                   Authentication authentication) {
        requireSameUser(username, authentication);
        return ResponseEntity.ok(priceAlertService.createAlert(username, request));
    }

    @DeleteMapping("/alerts/{username}/{alertId}")
    @Operation(summary = "Delete a price alert")
    public ResponseEntity<Void> deleteAlert(@PathVariable String username,
                                             @PathVariable Long alertId,
                                             Authentication authentication) {
        requireSameUser(username, authentication);
        priceAlertService.deleteAlert(username, alertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alerts/check")
    @Operation(summary = "Manually trigger alert evaluation")
    public List<PriceAlert> checkAlertsNow(Authentication authentication) {
        return priceAlertService.checkActiveAlerts(authentication.getName());
    }

    private void requireSameUser(String username, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getName().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user cannot access this resource.");
        }
    }
}
