package com.softpro.ATH.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DemandObservation {

    private final String productName;
    private final int quantity;
    private final BigDecimal pricePerUnit;
    private final LocalDateTime demandDate;
    private final String source;

    public DemandObservation(
            String productName,
            int quantity,
            BigDecimal pricePerUnit,
            LocalDateTime demandDate,
            String source) {

        this.productName = productName;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.demandDate = demandDate;
        this.source = source;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public LocalDateTime getDemandDate() {
        return demandDate;
    }

    public String getSource() {
        return source;
    }
}