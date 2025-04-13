package com.yassine.portfolio_tracker.controller;

import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.service.PortfolioService;
import org.springframework.web.bind.annotation.*;
import com.yassine.portfolio_tracker.service.StockService;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api")
public class PortfolioController {
    private final StockService stockService;
    private final PortfolioService portfolioService;

    public PortfolioController(StockService stockService, PortfolioService portfolioService) {
        this.stockService = stockService;
        this.portfolioService = portfolioService;
    }

    @GetMapping("/user/{username}")
    public Optional<Portfolio> getMyPortfolioData(@PathVariable String username) {
        return portfolioService.getPortfolioByUsername(username);
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
    public StockQuote getTopPerformer() {
        return stockService.getTopPerformer();
    }


}
