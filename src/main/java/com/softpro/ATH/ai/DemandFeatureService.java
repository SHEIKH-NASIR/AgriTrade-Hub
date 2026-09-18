package com.softpro.ATH.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Creates demand features from:
 *
 * 1. Real marketplace orders
 * 2. Historical demand records
 *
 * Both are converted into DemandObservation objects before
 * reaching this service.
 */
@Service
public class DemandFeatureService {


    /**
     * Builds historical ML training samples.
     */
    public List<ForecastFeature> buildTrainingSamples(
            List<DemandObservation> observations,
            LocalDateTime now) {

        List<ForecastFeature> samples =
                new ArrayList<>();

        if (observations == null
                || observations.isEmpty()) {

            return samples;
        }


        LocalDateTime firstAnchor =
                now.minusDays(365);

        LocalDateTime lastAnchor =
                now.minusDays(30);


        /*
         * Create a training window every 7 days.
         */
        for (LocalDateTime anchor = firstAnchor;
             !anchor.isAfter(lastAnchor);
             anchor = anchor.plusDays(7)) {


            double recent7 =
                    quantityBetween(
                            observations,
                            anchor.minusDays(7),
                            anchor
                    );


            double recent30 =
                    quantityBetween(
                            observations,
                            anchor.minusDays(30),
                            anchor
                    );


            double previous30 =
                    quantityBetween(
                            observations,
                            anchor.minusDays(60),
                            anchor.minusDays(30)
                    );


            double previous60 =
                    quantityBetween(
                            observations,
                            anchor.minusDays(90),
                            anchor.minusDays(60)
                    );


            int orderCount =
                    countBetween(
                            observations,
                            anchor.minusDays(30),
                            anchor
                    );


            double averageQuantity =
                    averageQuantityBetween(
                            observations,
                            anchor.minusDays(30),
                            anchor
                    );


            double averagePrice =
                    averagePriceBetween(
                            observations,
                            anchor.minusDays(30),
                            anchor
                    );


            double[] season =
                    seasonalFeatures(anchor);


            /*
             * Target = actual demand in the following 30 days.
             */
            double target =
                    quantityBetween(
                            observations,
                            anchor,
                            anchor.plusDays(30)
                    );


            /*
             * Ignore completely empty windows.
             */
            if (recent7 == 0
                    && recent30 == 0
                    && previous30 == 0
                    && previous60 == 0
                    && target == 0) {

                continue;
            }


            samples.add(
                    new ForecastFeature(
                            recent7,
                            recent30,
                            previous30,
                            previous60,
                            orderCount,
                            averageQuantity,
                            averagePrice,
                            season[0],
                            season[1],
                            target
                    )
            );
        }


        return samples;
    }


    /**
     * Current demand features.
     */
    public ForecastFeature buildCurrentFeature(
            List<DemandObservation> observations,
            LocalDateTime now) {

        if (observations == null) {
            observations = List.of();
        }


        double recent7 =
                quantityBetween(
                        observations,
                        now.minusDays(7),
                        now
                );


        double recent30 =
                quantityBetween(
                        observations,
                        now.minusDays(30),
                        now
                );


        double previous30 =
                quantityBetween(
                        observations,
                        now.minusDays(60),
                        now.minusDays(30)
                );


        double previous60 =
                quantityBetween(
                        observations,
                        now.minusDays(90),
                        now.minusDays(60)
                );


        int orderCount =
                countBetween(
                        observations,
                        now.minusDays(30),
                        now
                );


        double averageQuantity =
                averageQuantityBetween(
                        observations,
                        now.minusDays(30),
                        now
                );


        double averagePrice =
                averagePriceBetween(
                        observations,
                        now.minusDays(30),
                        now
                );


        double[] season =
                seasonalFeatures(now);


        return new ForecastFeature(
                recent7,
                recent30,
                previous30,
                previous60,
                orderCount,
                averageQuantity,
                averagePrice,
                season[0],
                season[1],
                0
        );
    }


    private double quantityBetween(
            List<DemandObservation> observations,
            LocalDateTime from,
            LocalDateTime to) {

        if (observations == null) {
            return 0;
        }


        return observations.stream()

                .filter(this::validObservation)

                .filter(observation ->
                        !observation.getDemandDate()
                                .isBefore(from))

                .filter(observation ->
                        observation.getDemandDate()
                                .isBefore(to))

                .mapToLong(
                        DemandObservation::getQuantity
                )

                .filter(quantity ->
                        quantity > 0)

                .sum();
    }


    private int countBetween(
            List<DemandObservation> observations,
            LocalDateTime from,
            LocalDateTime to) {

        if (observations == null) {
            return 0;
        }


        return (int) observations.stream()

                .filter(this::validObservation)

                .filter(observation ->
                        !observation.getDemandDate()
                                .isBefore(from))

                .filter(observation ->
                        observation.getDemandDate()
                                .isBefore(to))

                .count();
    }


    private double averageQuantityBetween(
            List<DemandObservation> observations,
            LocalDateTime from,
            LocalDateTime to) {

        if (observations == null) {
            return 0;
        }


        return observations.stream()

                .filter(this::validObservation)

                .filter(observation ->
                        !observation.getDemandDate()
                                .isBefore(from))

                .filter(observation ->
                        observation.getDemandDate()
                                .isBefore(to))

                .mapToInt(
                        DemandObservation::getQuantity
                )

                .filter(quantity ->
                        quantity > 0)

                .average()

                .orElse(0);
    }


    private double averagePriceBetween(
            List<DemandObservation> observations,
            LocalDateTime from,
            LocalDateTime to) {

        if (observations == null) {
            return 0;
        }


        return observations.stream()

                .filter(this::validObservation)

                .filter(observation ->
                        !observation.getDemandDate()
                                .isBefore(from))

                .filter(observation ->
                        observation.getDemandDate()
                                .isBefore(to))

                .map(DemandObservation::getPricePerUnit)

                .filter(price ->
                        price != null)

                .mapToDouble(
                        BigDecimal::doubleValue
                )

                .average()

                .orElse(0);
    }


    private boolean validObservation(
            DemandObservation observation) {

        if (observation == null) {
            return false;
        }

        if (observation.getDemandDate() == null) {
            return false;
        }

        if (observation.getQuantity() <= 0) {
            return false;
        }

        return true;
    }


    private double[] seasonalFeatures(
            LocalDateTime date) {

        int dayOfYear =
                date.getDayOfYear();

        int daysInYear =
                date.toLocalDate()
                        .lengthOfYear();

        double angle =
                2.0
                        * Math.PI
                        * dayOfYear
                        / daysInYear;


        return new double[] {
                Math.sin(angle),
                Math.cos(angle)
        };
    }
}