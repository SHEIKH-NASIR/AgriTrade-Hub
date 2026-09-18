package com.softpro.ATH.ai;

/**
 * Feature vector used by the demand forecasting model.
 *
 * These features are generated from the existing Order history.
 * No database changes are required.
 */
public class ForecastFeature {

    private final double recent7DayDemand;
    private final double recent30DayDemand;
    private final double previous30DayDemand;
    private final double previous60DayDemand;
    private final double orderCount30Days;
    private final double averageOrderQuantity;
    private final double averagePrice;
    private final double seasonSin;
    private final double seasonCos;

    /**
     * Target value used while training.
     * For the current prediction this remains 0.
     */
    private final double targetDemand;

    public ForecastFeature(
            double recent7DayDemand,
            double recent30DayDemand,
            double previous30DayDemand,
            double previous60DayDemand,
            double orderCount30Days,
            double averageOrderQuantity,
            double averagePrice,
            double seasonSin,
            double seasonCos,
            double targetDemand) {

        this.recent7DayDemand = recent7DayDemand;
        this.recent30DayDemand = recent30DayDemand;
        this.previous30DayDemand = previous30DayDemand;
        this.previous60DayDemand = previous60DayDemand;
        this.orderCount30Days = orderCount30Days;
        this.averageOrderQuantity = averageOrderQuantity;
        this.averagePrice = averagePrice;
        this.seasonSin = seasonSin;
        this.seasonCos = seasonCos;
        this.targetDemand = targetDemand;
    }

    public double getRecent7DayDemand() {
        return recent7DayDemand;
    }

    public double getRecent30DayDemand() {
        return recent30DayDemand;
    }

    public double getPrevious30DayDemand() {
        return previous30DayDemand;
    }

    public double getPrevious60DayDemand() {
        return previous60DayDemand;
    }

    public double getOrderCount30Days() {
        return orderCount30Days;
    }

    public double getAverageOrderQuantity() {
        return averageOrderQuantity;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    public double getSeasonSin() {
        return seasonSin;
    }

    public double getSeasonCos() {
        return seasonCos;
    }

    public double getTargetDemand() {
        return targetDemand;
    }

    /**
     * Converts the feature object into the numerical vector
     * used by the regression model.
     */
    public double[] toVector() {
        return new double[] {
                recent7DayDemand,
                recent30DayDemand,
                previous30DayDemand,
                previous60DayDemand,
                orderCount30Days,
                averageOrderQuantity,
                averagePrice,
                seasonSin,
                seasonCos
        };
    }
}