package com.yassine.portfolio_tracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BuyStockRequest {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
