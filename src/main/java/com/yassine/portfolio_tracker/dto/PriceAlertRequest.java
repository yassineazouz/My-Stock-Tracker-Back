package com.yassine.portfolio_tracker.dto;

import com.yassine.portfolio_tracker.model.AlertDirection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceAlertRequest {
    private String symbol;
    private String companyName;
    private double targetPrice;
    private AlertDirection direction;
}
