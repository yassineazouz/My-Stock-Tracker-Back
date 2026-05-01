package com.yassine.portfolio_tracker.controller;

import com.yassine.portfolio_tracker.dto.PriceAlertRequest;
import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.PriceAlert;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.service.PortfolioService;
import com.yassine.portfolio_tracker.service.PriceAlertService;
import com.yassine.portfolio_tracker.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
public class PortfolioController {
    private final StockService stockService;
    private final PortfolioService portfolioService;
    private final PriceAlertService priceAlertService;

    public PortfolioController(StockService stockService, PortfolioService portfolioService, PriceAlertService priceAlertService) {
        this.stockService = stockService;
        this.portfolioService = portfolioService;
        this.priceAlertService = priceAlertService;
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<Portfolio> getMyPortfolioData(@PathVariable String username) {
        return portfolioService.getPortfolioByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/user/stocks")
    public List<Stock> getMyPortfolioStocks() {
        return portfolioService.getAllUserStocks();
    }

    @DeleteMapping("/stocks/{id}")
    public void sellStock(@PathVariable Long id) {
        stockService.deleteStock(id);
    }

    @PostMapping("/user/stock/{username}")
    public Stock buyStock(@PathVariable String username, @RequestBody Stock stock) {
        return portfolioService.buyStock(stock, username);
    }

    @GetMapping("/stocks/all")
    public List<StockQuote> stocklist() {
        return stockService.getTopStockPrices();
    }

    @GetMapping("/stocks/top-performer")
    public ResponseEntity<StockQuote> getTopPerformer() {
        return stockService.getTopPerformer()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/alerts/{username}")
    public List<PriceAlert> getAlerts(@PathVariable String username) {
        return priceAlertService.getAlerts(username);
    }

    @PostMapping("/alerts/{username}")
    public ResponseEntity<PriceAlert> createAlert(
            @PathVariable String username,
            @RequestBody PriceAlertRequest request
    ) {
        return ResponseEntity.ok(priceAlertService.createAlert(username, request));
    }

    @DeleteMapping("/alerts/{username}/{alertId}")
    public ResponseEntity<Void> deleteAlert(@PathVariable String username, @PathVariable Long alertId) {
        priceAlertService.deleteAlert(username, alertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alerts/check")
    public List<PriceAlert> checkAlertsNow() {
        return priceAlertService.checkActiveAlerts();
    }

}
