package com.softpro.ATH.Dto;

import java.math.BigDecimal;

import com.softpro.ATH.Model.VehicleType;

public class MarketplaceOfferDto {

    private Long productId;

    private String productName;
    private String category;

    private String sellerName;
    private String sellerType;

    private BigDecimal productPrice;
    private int availableQuantity;

    private double roadDistanceKm;

    private VehicleType vehicleType;

    private BigDecimal logisticsCost;
    private BigDecimal landedCost;

    private boolean bestLandedCost;
    private String purchaseType;
    private String fairPriceStatus;
    private BigDecimal marketReferencePrice;
    private BigDecimal lowerFairPrice;
    private BigDecimal upperFairPrice;

    public MarketplaceOfferDto() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerType() {
        return sellerType;
    }

    public void setSellerType(String sellerType) {
        this.sellerType = sellerType;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(
            int availableQuantity) {

        this.availableQuantity = availableQuantity;
    }

    public double getRoadDistanceKm() {
        return roadDistanceKm;
    }

    public void setRoadDistanceKm(
            double roadDistanceKm) {

        this.roadDistanceKm = roadDistanceKm;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(
            VehicleType vehicleType) {

        this.vehicleType = vehicleType;
    }

    public BigDecimal getLogisticsCost() {
        return logisticsCost;
    }

    public void setLogisticsCost(
            BigDecimal logisticsCost) {

        this.logisticsCost = logisticsCost;
    }

    public BigDecimal getLandedCost() {
        return landedCost;
    }

    public void setLandedCost(
            BigDecimal landedCost) {

        this.landedCost = landedCost;
    }

    public String getFairPriceStatus() {
        return fairPriceStatus;
    }

    public void setFairPriceStatus(String fairPriceStatus) {
        this.fairPriceStatus = fairPriceStatus;
    }

    public BigDecimal getMarketReferencePrice() {
        return marketReferencePrice;
    }

    public void setMarketReferencePrice(BigDecimal marketReferencePrice) {
        this.marketReferencePrice = marketReferencePrice;
    }

    public BigDecimal getLowerFairPrice() {
        return lowerFairPrice;
    }

    public void setLowerFairPrice(BigDecimal lowerFairPrice) {
        this.lowerFairPrice = lowerFairPrice;
    }

    public BigDecimal getUpperFairPrice() {
        return upperFairPrice;
    }

    public void setUpperFairPrice(BigDecimal upperFairPrice) {
        this.upperFairPrice = upperFairPrice;
    }

    public String getPurchaseType() {
        return purchaseType;
    }

    public void setPurchaseType(String purchaseType) {
        this.purchaseType = purchaseType;
    }

    public boolean isBestLandedCost() {
        return bestLandedCost;
    }

    public void setBestLandedCost(
            boolean bestLandedCost) {

        this.bestLandedCost = bestLandedCost;
    }
}