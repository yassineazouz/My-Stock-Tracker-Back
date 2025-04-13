package com.yassine.portfolio_tracker.repository;

import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    // Custom query methods if needed
    Optional<Portfolio> findByOwner(String owner);

}
