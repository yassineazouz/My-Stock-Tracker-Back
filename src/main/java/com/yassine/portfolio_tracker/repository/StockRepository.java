package com.yassine.portfolio_tracker.repository;

import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    // Custom query methods if needed
    Stock findBySymbol(String symbol);

    Stock findBySymbolAndPortfolioId(String symbol, Long portfolioId);

}
