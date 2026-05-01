package com.yassine.portfolio_tracker.dto;

import lombok.Data;

@Data
public class StockQuote {
    private String symbol;
    private String name;
    private Double close;
    private Double previousClose;
    private Double change;
    private Double percentChange;
    private Integer volume;
}
