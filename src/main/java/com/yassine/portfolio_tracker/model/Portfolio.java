package com.yassine.portfolio_tracker.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner;

    private double totalValue;

    private Double walletValue;


    private double initialInvestment;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "portfolio_id")
    @JsonManagedReference("portfolio-stocks")
    @Builder.Default
    private List<Stock> stocks = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("portfolio-alerts")
    @Builder.Default
    private List<PriceAlert> alerts = new ArrayList<>();

    private int activeAlerts;

    public double getPerformancePercent() {
        if (initialInvestment == 0) return 0;
        return ((getTotalValue() - initialInvestment) / initialInvestment) * 100;
    }

    public double getTotalValue() {
        if (stocks == null) return 0;
        return stocks.stream()
                .mapToDouble(stock -> stock.getCurrentPrice() * stock.getQuantity())
                .sum();
    }
}
