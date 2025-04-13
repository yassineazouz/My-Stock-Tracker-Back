package com.yassine.portfolio_tracker.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = true)
    private Double walletValue;


    private double initialInvestment;

    @Getter
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "portfolio_id") // Foreign key in Stock table
    @JsonManagedReference
    private List<Stock> stocks;

    private int activeAlerts;

    public double getPerformancePercent() {
        if (initialInvestment == 0) return 0;
        return ((getTotalValue() - initialInvestment) / initialInvestment) * 100;
    }

    public double getTotalValue() {
        return stocks.stream()
                .mapToDouble(stock -> stock.getCurrentPrice() * stock.getQuantity())
                .sum();
    }
}