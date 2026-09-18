package com.softpro.ATH.ai;

public class DemandForecastResponse {

    private String productName;
    private long forecastQuantity;
    private long currentSupply;
    private long gap;
    private String trend;
    private String recommendation;

    /*
     * New AI/ML information.
     */
    private int confidence;
    private String modelName;
    private String forecastPeriod;

    public DemandForecastResponse() {
    }

    public DemandForecastResponse(
            String productName,
            long forecastQuantity,
            long currentSupply,
            long gap,
            String trend,
            String recommendation) {

        this(
                productName,
                forecastQuantity,
                currentSupply,
                gap,
                trend,
                recommendation,
                0,
                "ATH AI Demand Regression v1",
                "Next 30 days"
        );
    }

    public DemandForecastResponse(
            String productName,
            long forecastQuantity,
            long currentSupply,
            long gap,
            String trend,
            String recommendation,
            int confidence,
            String modelName,
            String forecastPeriod) {

        this.productName = productName;
        this.forecastQuantity = forecastQuantity;
        this.currentSupply = currentSupply;
        this.gap = gap;
        this.trend = trend;
        this.recommendation = recommendation;
        this.confidence = confidence;
        this.modelName = modelName;
        this.forecastPeriod = forecastPeriod;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getForecastQuantity() {
        return forecastQuantity;
    }

    public void setForecastQuantity(long forecastQuantity) {
        this.forecastQuantity = forecastQuantity;
    }

    public long getCurrentSupply() {
        return currentSupply;
    }

    public void setCurrentSupply(long currentSupply) {
        this.currentSupply = currentSupply;
    }

    public long getGap() {
        return gap;
    }

    public void setGap(long gap) {
        this.gap = gap;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getForecastPeriod() {
        return forecastPeriod;
    }

    public void setForecastPeriod(String forecastPeriod) {
        this.forecastPeriod = forecastPeriod;
    }
}