package com.softpro.ATH.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.softpro.ATH.Dto.LogisticsCostResponse;
import com.softpro.ATH.Model.VehicleType;

@Service
public class LogisticsCostService {

    private final GeocodingService geocodingService;
    private final RouteOptimizationService routeOptimizationService;

    public LogisticsCostService(GeocodingService geocodingService,
                                RouteOptimizationService routeOptimizationService) {
        this.geocodingService = geocodingService;
        this.routeOptimizationService = routeOptimizationService;
    }

    /** Calculates logistics from pickup/destination addresses using geocoding + ORS road routing. */
    public LogisticsCostResponse calculateFromAddresses(int quantityKg, String pickupAddress, String destinationAddress) {
        if (pickupAddress == null || pickupAddress.isBlank())
            return LogisticsCostResponse.failure("Pickup address is missing.");
        if (destinationAddress == null || destinationAddress.isBlank())
            return LogisticsCostResponse.failure("Merchant destination address is missing.");
        try {
            GeocodingService.Coordinates pickup = geocodingService.geocode(pickupAddress.trim(), false);
            GeocodingService.Coordinates destination = geocodingService.geocode(destinationAddress.trim(), false);
            if (pickup == null || destination == null)
                return LogisticsCostResponse.failure("Unable to geocode pickup or destination address.");
            double[] route = routeOptimizationService.calculateDirectRoadRoute(
                    pickup.getLatitude(), pickup.getLongitude(),
                    destination.getLatitude(), destination.getLongitude());
            return calculateFromRoadDistance(quantityKg, route[0], route[1]);
        } catch (Exception e) {
            return LogisticsCostResponse.failure(e.getMessage());
        }
    }

    /**
     * Calculates logistics cost after an actual road distance
     * has been obtained from OpenRouteService.
     *
     * IMPORTANT:
     * roadDistanceKm MUST come from ORS road routing.
     * Do not pass a straight-line/Haversine distance here.
     */
    public LogisticsCostResponse calculateFromRoadDistance(
            int quantityKg,
            double roadDistanceKm,
            double durationMinutes) {

        if (quantityKg <= 0) {

            return LogisticsCostResponse.failure(
                    "Quantity must be greater than zero."
            );
        }

        if (!Double.isFinite(roadDistanceKm)
                || roadDistanceKm < 0) {

            return LogisticsCostResponse.failure(
                    "Invalid road distance."
            );
        }

        VehicleType vehicle;

        try {

            vehicle =
                    VehicleType.forQuantity(
                            quantityKg
                    );

        } catch (IllegalArgumentException e) {

            return LogisticsCostResponse.failure(
                    e.getMessage()
            );
        }

        BigDecimal baseCost =
                BigDecimal.valueOf(
                        vehicle.getBaseCost()
                ).setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal distanceCost =
                BigDecimal.valueOf(
                        vehicle.getCostPerKm()
                                * roadDistanceKm
                ).setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal logisticsCost =
                baseCost
                        .add(distanceCost)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return LogisticsCostResponse.success(
                roundDistance(roadDistanceKm),
                durationMinutes,
                vehicle,
                quantityKg,
                baseCost,
                distanceCost,
                logisticsCost
        );
    }

    private double roundDistance(
            double distanceKm) {

        return BigDecimal
                .valueOf(distanceKm)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }
}