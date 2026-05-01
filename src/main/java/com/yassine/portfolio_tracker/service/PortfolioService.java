package com.yassine.portfolio_tracker.service;

import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.repository.PortfolioRepository;
import com.yassine.portfolio_tracker.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Stock buyStock(Stock stock, String username) {
        Portfolio portfolio = portfolioRepository.findByOwner(username)
                .orElseThrow(() -> new RuntimeException("Portfolio not found for user: " + username));

        double purchaseCost = stock.getCurrentPrice() * stock.getQuantity();

        if (portfolio.getWalletValue() < purchaseCost) {
            throw new RuntimeException("Not enough funds in wallet to buy this stock.");
        }

        Optional<Stock> existing = stockRepository.findBySymbolAndPortfolioId(stock.getSymbol(), portfolio.getId());

        if (existing.isPresent()) {
            Stock existingStock = existing.get();
            int newQuantity = existingStock.getQuantity() + stock.getQuantity();
            double newTotalCost = existingStock.getPurchasePrice() * existingStock.getQuantity()
                    + stock.getPurchasePrice() * stock.getQuantity();
            double newAvgPrice = newTotalCost / newQuantity;

            existingStock.setQuantity(newQuantity);
            existingStock.setPurchasePrice(newAvgPrice);

            portfolio.setWalletValue(portfolio.getWalletValue() - purchaseCost);
            portfolioRepository.save(portfolio);
            return stockRepository.save(existingStock);
        } else {
            stock.setPortfolio(portfolio);
            portfolio.setWalletValue(portfolio.getWalletValue() - purchaseCost);
            portfolioRepository.save(portfolio);
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