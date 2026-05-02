package com.yassine.portfolio_tracker.repository;

import com.yassine.portfolio_tracker.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByPortfolioOwnerOrderByCreatedAtDesc(String owner);
    List<PriceAlert> findByActiveTrueOrderByCreatedAtAsc();
    List<PriceAlert> findByPortfolioOwnerAndActiveTrueOrderByCreatedAtAsc(String owner);
    long countByPortfolioOwnerAndActiveTrue(String owner);
}
