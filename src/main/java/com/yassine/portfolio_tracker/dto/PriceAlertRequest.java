package com.yassine.portfolio_tracker.dto;

import com.yassine.portfolio_tracker.model.AlertDirection;
import lombok.Data;

@Data
public class PriceAlertRequest {
    private String symbol;
    private String companyName;
    private double targetPrice;
    private AlertDirection direction;
}
