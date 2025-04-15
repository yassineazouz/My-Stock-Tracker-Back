package com.yassine.portfolio_tracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCache {
    @Id
    private String symbol;
    private String name;
    private Double close;
    private Double previousClose;
    private Double change;
    private Double percentChange;
    private Integer volume;
}
