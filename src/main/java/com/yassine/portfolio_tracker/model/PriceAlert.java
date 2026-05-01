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
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    private String companyName;

    @Column(nullable = false)
    private double targetPrice;

    private double currentPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertDirection direction;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastCheckedAt;

    private Instant triggeredAt;

    private Double triggeredPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference("portfolio-alerts")
    private Portfolio portfolio;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        active = true;
        symbol = symbol == null ? null : symbol.trim().toUpperCase();
    }
}
