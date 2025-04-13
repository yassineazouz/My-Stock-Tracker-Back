package com.yassine.portfolio_tracker.service;

import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.repository.PortfolioRepository;
import com.yassine.portfolio_tracker.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PortfolioService {

    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;

    public PortfolioService(StockRepository stockRepository, PortfolioRepository portfolioRepository) {
        this.stockRepository = stockRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public List<Stock> getAllUserStocks() {
        return stockRepository.findAll();
    }


    public Optional<Stock> getStockById(Long id) {
        return stockRepository.findById(id);
    }
    public Stock buyStock(Stock stock, String username) {
        Portfolio portfolio = portfolioRepository.findByOwner(username)
                .orElseThrow(() -> new RuntimeException("Portfolio not found for user: " + username));

        double purchaseCost = stock.getCurrentPrice() * stock.getQuantity();

        if (portfolio.getWalletValue() < purchaseCost) {
            throw new RuntimeException("Not enough funds in wallet to buy this stock.");
        }

        Stock existingStock = stockRepository.findBySymbolAndPortfolioId(stock.getSymbol(), portfolio.getId());

        if (existingStock != null) {
            // Update quantity and price (average if needed)
            int newQuantity = existingStock.getQuantity() + stock.getQuantity();
            double newTotalCost = existingStock.getPurchasePrice() * existingStock.getQuantity()
                    + stock.getPurchasePrice() * stock.getQuantity();
            double newAvgPrice = newTotalCost / newQuantity;

            existingStock.setQuantity(newQuantity);
            existingStock.setPurchasePrice(newAvgPrice);

            portfolio.setWalletValue(portfolio.getWalletValue() - purchaseCost);
            return stockRepository.save(existingStock);
        } else {
            // New stock
            stock.setPortfolio(portfolio);
            portfolio.getStocks().add(stock);

            portfolio.setWalletValue(portfolio.getWalletValue() - purchaseCost);
            return stockRepository.save(stock);
        }
    }



    public void deleteStock(Long id) {
        stockRepository.deleteById(id);
    }

    public Optional<Portfolio> getPortfolioByUsername(String username) {
        return portfolioRepository.findByOwner(username);
    }
}