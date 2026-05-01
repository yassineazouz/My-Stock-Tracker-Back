package com.yassine.portfolio_tracker.dto;

import com.yassine.portfolio_tracker.model.AlertDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PriceAlertRequest {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    private String companyName;

    @Positive(message = "Target price must be greater than zero")
    private double targetPrice;

    @NotNull(message = "Direction is required")
    private AlertDirection direction;
}
