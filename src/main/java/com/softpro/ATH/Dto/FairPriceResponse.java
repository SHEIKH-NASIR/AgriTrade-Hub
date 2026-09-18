package com.softpro.ATH.Dto;

import java.math.BigDecimal;

public class FairPriceResponse {

    private boolean referenceAvailable;
    private boolean fair;

    private String status;
    private String message;

    private BigDecimal referencePrice;
    private BigDecimal lowerFairPrice;
    private BigDecimal upperFairPrice;
    private BigDecimal sellerPrice;

    public FairPriceResponse() {
    }

    public boolean isReferenceAvailable() {
        return referenceAvailable;
    }

    public void setReferenceAvailable(
            boolean referenceAvailable) {

        this.referenceAvailable =
                referenceAvailable;
    }

    public boolean isFair() {
        return fair;
    }

    public void setFair(boolean fair) {
        this.fair = fair;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getReferencePrice() {
        return referencePrice;
    }

    public void setReferencePrice(
            BigDecimal referencePrice) {

        this.referencePrice = referencePrice;
    }

    public BigDecimal getLowerFairPrice() {
        return lowerFairPrice;
    }

    public void setLowerFairPrice(
            BigDecimal lowerFairPrice) {

        this.lowerFairPrice = lowerFairPrice;
    }

    public BigDecimal getUpperFairPrice() {
        return upperFairPrice;
    }

    public void setUpperFairPrice(
            BigDecimal upperFairPrice) {

        this.upperFairPrice = upperFairPrice;
    }

    public BigDecimal getSellerPrice() {
        return sellerPrice;
    }

    public void setSellerPrice(
            BigDecimal sellerPrice) {

        this.sellerPrice = sellerPrice;
    }
}