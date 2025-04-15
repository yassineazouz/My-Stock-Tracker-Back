package com.yassine.portfolio_tracker.repository;

import com.yassine.portfolio_tracker.model.StockCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockCacheRepository extends JpaRepository<StockCache, Long> {
    Optional<StockCache> findBySymbol(String symbol);

}
