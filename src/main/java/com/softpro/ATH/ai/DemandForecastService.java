package com.softpro.ATH.ai;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.FPOProduct;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.ForecastDemandHistory;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Product;
import com.softpro.ATH.Repository.FPOProductRepo;
import com.softpro.ATH.Repository.ForecastDemandHistoryRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.ProductRepo;

@Service
public class DemandForecastService {

    private static final String FORECAST_PERIOD =
            "Next 30 days";


    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private FPOProductRepo fpoProductRepo;

    @Autowired
    private ForecastDemandHistoryRepo historyRepo;

    @Autowired
    private DemandFeatureService demandFeatureService;

    @Autowired
    private AIForecastEngine aiForecastEngine;


    /**
     * Independent Farmer forecast.
     */
    public List<DemandForecastResponse> forecastForFarmer(
            Formers farmer) {

        if (farmer == null) {
            return List.of();
        }


        List<Order> orders =
                orderRepo.findByFarmerAndDemoOrder(farmer, false);


        List<Product> products =
                productRepo.findByFarmer(farmer);


        List<ForecastDemandHistory> history =
                historyRepo.findByFarmerOrderByDemandDateAsc(
                        farmer
                );


        Map<String, Long> supply =
                new HashMap<>();


        for (Product product : products) {

            if (product == null) {
                continue;
            }


            if (product.getStatus() == null
                    || !"Available".equalsIgnoreCase(
                    product.getStatus())
                    || product.getQuantity() <= 0) {

                continue;
            }


            /*
             * FPO-selling products remain excluded from
             * independent farmer forecasting.
             */
            if (product.getSellingMode() != null
                    && "FPO".equalsIgnoreCase(
                    product.getSellingMode())) {

                continue;
            }


            String key =
                    normalize(
                            product.getProductName()
                    );


            supply.merge(
                    key,
                    (long) product.getQuantity(),
                    Long::sum
            );
        }


        return buildForecast(
                orders,
                history,
                supply,
                false
        );
    }


    /**
     * FPO forecast.
     */
    public List<DemandForecastResponse> forecastForFpo(
            FPO fpo) {

        if (fpo == null) {
            return List.of();
        }


        List<Order> orders =
                orderRepo.findByFpoAndDemoOrder(fpo, false);


        List<FPOProduct> products =
                fpoProductRepo.findByFpo(fpo);


        List<ForecastDemandHistory> history =
                historyRepo.findByFpoOrderByDemandDateAsc(
                        fpo
                );


        Map<String, Long> supply =
                new HashMap<>();


        for (FPOProduct product : products) {

            if (product == null) {
                continue;
            }


            if (product.getStatus() == null
                    || !"Available".equalsIgnoreCase(
                    product.getStatus())
                    || product.getAvailableQuantity() <= 0) {

                continue;
            }


            String key =
                    normalize(
                            product.getProductName()
                    );


            supply.merge(
                    key,
                    (long) product.getAvailableQuantity(),
                    Long::sum
            );
        }


        return buildForecast(
                orders,
                history,
                supply,
                true
        );
    }


    /**
     * Builds forecast using BOTH:
     *
     * Real marketplace orders
     * +
     * Historical demand records
     */
    private List<DemandForecastResponse> buildForecast(
            List<Order> orders,
            List<ForecastDemandHistory> history,
            Map<String, Long> currentSupply,
            boolean fpoForecast) {

        if (orders == null) {
            orders = List.of();
        }

        if (history == null) {
            history = List.of();
        }

        if (currentSupply == null) {
            currentSupply = Map.of();
        }


        LocalDateTime now =
                LocalDateTime.now();


        /*
         * Convert real orders into the common AI observation format.
         */
        List<DemandObservation> observations =
                new ArrayList<>();


        for (Order order : orders) {

            if (order == null) {
                continue;
            }


            if (order.getProductName() == null
                    || order.getProductName().isBlank()) {

                continue;
            }


            if (order.getOrderDate() == null) {
                continue;
            }


            if ("Cancelled".equalsIgnoreCase(
                    order.getOrderStatus())) {

                continue;
            }


            if (order.getQuantity() <= 0) {
                continue;
            }


            observations.add(
                    new DemandObservation(
                            order.getProductName(),
                            order.getQuantity(),
                            order.getPricePerUnit(),
                            order.getOrderDate(),
                            "REAL_ORDER"
                    )
            );
        }


        /*
         * Add historical demand records.
         */
        for (ForecastDemandHistory item : history) {

            if (item == null) {
                continue;
            }


            if (item.getProductName() == null
                    || item.getProductName().isBlank()) {

                continue;
            }


            if (item.getDemandDate() == null) {
                continue;
            }


            if (item.getQuantity() <= 0) {
                continue;
            }


            LocalDateTime demandDate =
                    item.getDemandDate()
                            .atStartOfDay();


            observations.add(
                    new DemandObservation(
                            item.getProductName(),
                            item.getQuantity(),
                            item.getPricePerUnit(),
                            demandDate,
                            "HISTORICAL"
                    )
            );
        }


        /*
         * Group all demand by product.
         */
        Map<String, List<DemandObservation>>
                demandByProduct =
                observations.stream()

                        .collect(
                                Collectors.groupingBy(
                                        observation ->
                                                normalize(
                                                        observation
                                                                .getProductName()
                                                )
                                )
                        );


        /*
         * Products shown on the forecast page come from:
         *
         * 1. Real orders
         * 2. Historical demand
         * 3. Current supply
         */
        Set<String> productKeys =
                new LinkedHashSet<>();


        productKeys.addAll(
                demandByProduct.keySet()
        );


        productKeys.addAll(
                currentSupply.keySet()
        );


        List<DemandForecastResponse> result =
                new ArrayList<>();


        for (String productKey : productKeys) {

            List<DemandObservation>
                    productDemand =
                    demandByProduct.getOrDefault(
                            productKey,
                            List.of()
                    );


            /*
             * AI training samples.
             */
            List<ForecastFeature>
                    trainingSamples =
                    demandFeatureService
                            .buildTrainingSamples(
                                    productDemand,
                                    now
                            );


            /*
             * Current demand features.
             */
            ForecastFeature currentFeature =
                    demandFeatureService
                            .buildCurrentFeature(
                                    productDemand,
                                    now
                            );


            /*
             * AI prediction.
             */
            AIDemandPrediction prediction =
                    aiForecastEngine.predict(
                            trainingSamples,
                            currentFeature
                    );


            long forecast =
                    prediction.getPredictedDemand();


            long supply =
                    currentSupply.getOrDefault(
                            productKey,
                            0L
                    );


            long gap =
                    forecast - supply;


            String trend =
                    calculateTrend(
                            productDemand,
                            now
                    );


            /*
             * Product display name.
             */
            String displayName =
                    productDemand.stream()

                            .map(
                                    DemandObservation
                                            ::getProductName
                            )

                            .filter(
                                    name ->
                                            name != null
                                                    && !name.isBlank()
                            )

                            .findFirst()

                            .orElse(
                                    productKey
                            );


            String recommendation =
                    buildRecommendation(
                            displayName,
                            gap,
                            fpoForecast
                    );


            result.add(
                    new DemandForecastResponse(
                            displayName,
                            forecast,
                            supply,
                            gap,
                            trend,
                            recommendation,
                            prediction.getConfidence(),
                            prediction.getModelName(),
                            FORECAST_PERIOD
                    )
            );
        }


        result.sort(
                Comparator.comparing(
                        DemandForecastResponse::getProductName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );


        return result;
    }


    /**
     * Calculates demand trend using real + historical demand.
     */
    private String calculateTrend(
            List<DemandObservation> observations,
            LocalDateTime now) {

        long recent =
                quantityBetween(
                        observations,
                        now.minusDays(30),
                        now
                );


        long previous =
                quantityBetween(
                        observations,
                        now.minusDays(60),
                        now.minusDays(30)
                );


        if (recent == 0 && previous == 0) {
            return "→ No history";
        }


        if (previous == 0) {

            return recent > 0
                    ? "↑ Increasing"
                    : "→ Stable";
        }


        double change =
                ((double) recent - previous)
                        / previous;


        if (change >= 0.15) {
            return "↑ Increasing";
        }


        if (change <= -0.15) {
            return "↓ Decreasing";
        }


        return "→ Stable";
    }


    private long quantityBetween(
            List<DemandObservation> observations,
            LocalDateTime from,
            LocalDateTime to) {

        if (observations == null) {
            return 0;
        }


        return observations.stream()

                .filter(
                        observation ->
                                observation != null
                )

                .filter(
                        observation ->
                                observation.getDemandDate()
                                        != null
                )

                .filter(
                        observation ->
                                !observation
                                        .getDemandDate()
                                        .isBefore(from)
                )

                .filter(
                        observation ->
                                observation
                                        .getDemandDate()
                                        .isBefore(to)
                )

                .mapToLong(
                        DemandObservation
                                ::getQuantity
                )

                .filter(
                        quantity ->
                                quantity > 0
                )

                .sum();
    }


    private String buildRecommendation(
            String productName,
            long gap,
            boolean fpoForecast) {

        if (gap > 0) {

            if (fpoForecast) {

                return "AI recommends coordinating approximately "
                        + gap
                        + " kg additional "
                        + productName
                        + " contribution from member farmers.";
            }


            return "AI recommends increasing "
                    + productName
                    + " supply by approximately "
                    + gap
                    + " kg.";
        }


        if (gap < 0) {

            return "Current "
                    + productName
                    + " supply exceeds AI forecast by approximately "
                    + Math.abs(gap)
                    + " kg.";
        }


        return "Current "
                + productName
                + " supply is aligned with AI forecast demand.";
    }


    private String normalize(String value) {

        return value == null
                ? ""
                : value.trim().toLowerCase();
    }
}