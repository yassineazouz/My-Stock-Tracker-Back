package com.yassine.portfolio_tracker.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "portfolio")
@ToString(exclude = "portfolio")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String companyName;
    private int quantity;
    private double purchasePrice;
    private double currentPrice;

    @Column(nullable = false, updatable = false)
    private Instant purchaseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference("portfolio-stocks")
    private Portfolio portfolio;

    public double getTotalValue() {
        return quantity * currentPrice;
    }

    @PrePersist
    protected void onCreate() {
        purchaseDate = Instant.now();
    }
}
