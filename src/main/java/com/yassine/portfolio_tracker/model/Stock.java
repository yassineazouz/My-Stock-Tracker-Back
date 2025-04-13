package com.yassine.portfolio_tracker.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String companyName;
    private int quantity;
    private double purchasePrice;
    private double currentPrice;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date purchaseDate;
    @ManyToOne
    @JsonBackReference
    private Portfolio portfolio;


    public double getTotalValue() {
        return quantity * currentPrice;
    }
    @PrePersist
    protected void onCreate() {
        this.purchaseDate = new Date();
    }


}
