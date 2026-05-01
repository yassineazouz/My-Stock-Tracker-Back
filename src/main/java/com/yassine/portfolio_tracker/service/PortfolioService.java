package com.yassine.portfolio_tracker.service;

import com.yassine.portfolio_tracker.dto.BuyStockRequest;
import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.repository.PortfolioRepository;
import com.yassine.portfolio_tracker.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PortfolioService {

    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockService stockService;

    public PortfolioService(StockRepository stockRepository,
                            PortfolioRepository portfolioRepository,
                            StockService stockService) {
        this.stockRepository = stockRepository;
        this.portfolioRepository = portfolioRepository;
        this.stockService = stockService;
    }

    public List<Stock> getAllUserStocks() {
        return stockRepository.findAll();
    }

    public Optional<Portfolio> getPortfolioByUsername(String username) {
        return portfolioRepository.findByOwner(username);
    }

    @Transactional
    public Stock buyStock(BuyStockRequest request, String username) {
        Portfolio portfolio = portfolioRepository.findByOwner(username)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found for user: " + username));

        String symbol = request.getSymbol().trim().toUpperCase();

        StockQuote quote = stockService.getQuote(symbol)
                .orElseThrow(() -> new IllegalArgumentException("No price data available for: " + symbol));

        double currentPrice = quote.getClose();
        double purchaseCost = currentPrice * request.getQuantity();

        if (portfolio.getWalletValue() == null || portfolio.getWalletValue() < purchaseCost) {
            throw new IllegalArgumentException("Not enough funds in wallet to buy this stock.");
        }

        Optional<Stock> existing = stockRepository.findBySymbolAndPortfolioId(symbol, portfolio.getId());

        if (existing.isPresent()) {
            Stock existingStock = existing.get();
            int newQuantity = existingStock.getQuantity() + request.getQuantity();
            double newAvgPrice = (existingStock.getPurchasePrice() * existingStock.getQuantity()
                    + currentPrice * request.getQuantity()) / newQuantity;

            existingStock.setQuantity(newQuantity);
            existingStock.setPurchasePrice(newAvgPrice);
            existingStock.setCurrentPrice(currentPrice);

            portfolio.setWalletValue(portfolio.getWalletValue() - purchaseCost);
            portfolioRepository.save(portfolio);
            return stockRepository.save(existingStock);
        }

        Stock stock = Stock.builder()
                .symbol(symbol)
                .companyName(request.getCompanyName())
                .quantity(request.getQuantity())
                .purchasePrice(currentPrice)
                .currentPrice(currentPrice)
                .portfolio(portfolio)
                .build();

        portfolio.setWalletValue(portfolio.getWalletValue() - purchaseCost);
        portfolioRepository.save(portfolio);
        return stockRepository.save(stock);
    }

    @Transactional
    public void sellStock(Long stockId, String username) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));

        Portfolio portfolio = stock.getPortfolio();
        if (portfolio == null || !portfolio.getOwner().equals(username)) {
            throw new IllegalArgumentException("Stock does not belong to user: " + username);
        }

        double saleValue = stock.getCurrentPrice() * stock.getQuantity();
        portfolio.setWalletValue(portfolio.getWalletValue() + saleValue);
        portfolioRepository.save(portfolio);

        stockRepository.delete(stock);
        log.info("Sold {} x {} for {} — wallet credited", stock.getQuantity(), stock.getSymbol(), saleValue);
    }
}
