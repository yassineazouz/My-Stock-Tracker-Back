package com.yassine.portfolio_tracker.repository;

import com.yassine.portfolio_tracker.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbol(String symbol);
    Optional<Stock> findBySymbolAndPortfolioId(String symbol, Long portfolioId);

}
