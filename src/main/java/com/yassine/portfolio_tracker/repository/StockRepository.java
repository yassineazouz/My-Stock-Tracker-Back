package com.yassine.portfolio_tracker.repository;

import com.yassine.portfolio_tracker.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;


public interface StockRepository extends JpaRepository<Stock, Long> {
    Stock findBySymbol(String symbol);
    Stock findBySymbolAndPortfolioId(String symbol, Long portfolioId);

}
