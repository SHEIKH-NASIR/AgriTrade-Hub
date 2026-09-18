package com.softpro.ATH.Model;

public enum VehicleType {

    MINI_TRUCK(1000, 800, 18, 15),
    SMALL_TRUCK(3000, 2500, 28, 20),
    MEDIUM_TRUCK(7000, 6000, 45, 28),
    LARGE_TRUCK(15000, 12000, 70, 35);

    private final int capacityKg;
    private final int recommendedLoadKg;
    private final double baseCost;
    private final double costPerKm;

    VehicleType(
            int capacityKg,
            int recommendedLoadKg,
            double baseCost,
            double costPerKm) {

        this.capacityKg = capacityKg;
        this.recommendedLoadKg = recommendedLoadKg;
        this.baseCost = baseCost;
        this.costPerKm = costPerKm;
    }

    public int getCapacityKg() {
        return capacityKg;
    }

    public int getRecommendedLoadKg() {
        return recommendedLoadKg;
    }

    public double getBaseCost() {
        return baseCost;
    }

    public double getCostPerKm() {
        return costPerKm;
    }

    /**
     * Selects the smallest vehicle capable of carrying
     * the requested quantity.
     */
    public static VehicleType forQuantity(int quantityKg) {

        if (quantityKg <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }

        for (VehicleType vehicle : values()) {

            if (quantityKg <= vehicle.capacityKg) {
                return vehicle;
            }
        }

        throw new IllegalArgumentException(
                "Requested quantity exceeds the maximum supported vehicle capacity."
        );
    }
}