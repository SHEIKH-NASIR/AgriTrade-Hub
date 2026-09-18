package com.softpro.ATH.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softpro.ATH.Dto.RouteOptimizationResponse;
import com.softpro.ATH.Dto.RouteStop;
import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Repository.FPOShipmentRepo;

/**
 * Road-distance delivery route optimizer.
 *
 * Uses OpenRouteService (ORS) Matrix API.
 * NO Haversine distance is used.
 * NO straight-line distance is used.
 *
 * Algorithm:
 *
 *   Pickup
 *      |
 *      +--> nearest remaining destination by ROAD distance
 *                    |
 *                    +--> nearest remaining destination by ROAD distance
 *                                  |
 *                                  +--> repeat
 *
 * ORS returns distances from its driving road network in meters.
 */
@Service
public class RouteOptimizationService {

    private static class MatrixData {

        private final double[][] distancesKm;
        private final double[][] durationsSeconds;

        private MatrixData(
                double[][] distancesKm,
                double[][] durationsSeconds) {

            this.distancesKm = distancesKm;
            this.durationsSeconds = durationsSeconds;
        }
    }

    private static final String ORS_MATRIX_URL =
            "https://api.openrouteservice.org/v2/matrix/driving-car";

    private final FPOShipmentRepo shipmentRepo;
    private final GeocodingService geocodingService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ors.api.key}")
    private String orsApiKey;

    public RouteOptimizationService(
            FPOShipmentRepo shipmentRepo,
            GeocodingService geocodingService) {

        this.shipmentRepo = shipmentRepo;
        this.geocodingService = geocodingService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public RouteOptimizationResponse optimizeRoute(
            List<FPOShipment> shipments) {

        RouteOptimizationResponse response =
                new RouteOptimizationResponse();

        if (shipments == null || shipments.isEmpty()) {
            response.setMessage(
                    "No shipments available for route optimization.");
            return response;
        }

        if (orsApiKey == null || orsApiKey.isBlank()
                || "YOUR_NEW_ORS_KEY_HERE".equals(orsApiKey.trim())) {

            response.setMessage(
                    "OpenRouteService API key is missing. "
                    + "Set ors.api.key in application.properties.");
            return response;
        }

        List<FPOShipment> validShipments = new ArrayList<>();

        /*
         * Re-geocode missing coordinates.
         *
         * We DO NOT calculate distance here.
         * ORS is the only source used for road distance.
         */
        for (FPOShipment shipment : shipments) {

            if (shipment == null) {
                continue;
            }

            boolean changed = false;

            // MS Hospital is a known local POI override. Existing shipments
            // may contain stale coordinates saved before the override was
            // corrected, so always refresh this specific destination before
            // sending coordinates to ORS. Other destinations keep their
            // existing coordinates unless they are missing/invalid.
            boolean knownMsHospital = isKnownMsHospitalAddress(
                    shipment.getDestinationAddress());

            if (knownMsHospital || !hasValidDestinationCoordinates(shipment)) {

                String address = shipment.getDestinationAddress();

                if (address != null && !address.isBlank()) {

                    try {
                        GeocodingService.Coordinates c =
                                geocodingService.geocode(address.trim(), false);

                        if (c != null) {
                            shipment.setDestinationLatitude(
                                    c.getLatitude());
                            shipment.setDestinationLongitude(
                                    c.getLongitude());
                            applyGeocodeMetadata(shipment, c);
                            changed = true;
                        }
                    } catch (Exception e) {
                        System.out.println(
                                "Destination geocoding failed for shipment "
                                        + shipment.getId()
                                        + ": "
                                        + e.getMessage());
                    }
                }
            }

            if (!hasValidPickupCoordinates(shipment)) {

                String address = shipment.getPickupAddress();

                if (address != null && !address.isBlank()) {

                    try {
                        GeocodingService.Coordinates c =
                                geocodingService.geocode(address.trim(), false);

                        if (c != null) {
                            shipment.setPickupLatitude(c.getLatitude());
                            shipment.setPickupLongitude(c.getLongitude());
                            applyGeocodeMetadata(shipment, c);
                            changed = true;
                        }
                    } catch (Exception e) {
                        System.out.println(
                                "Pickup geocoding failed for shipment "
                                        + shipment.getId()
                                        + ": "
                                        + e.getMessage());
                    }
                }
            }

            if (changed) {
                shipmentRepo.saveAndFlush(shipment);
            }

            if (hasValidDestinationCoordinates(shipment)) {
                validShipments.add(shipment);
            }
        }

        if (validShipments.isEmpty()) {
            response.setMessage(
                    "No shipment destination has valid coordinates. "
                            + "Please check the destination addresses.");
            return response;
        }

        /*
         * Find the actual pickup point.
         */
        FPOShipment pickupShipment = null;

        for (FPOShipment shipment : validShipments) {

            if (hasValidPickupCoordinates(shipment)) {
                pickupShipment = shipment;
                break;
            }
        }

        if (pickupShipment == null) {
            response.setMessage(
                    "Pickup coordinates could not be determined. "
                            + "Please check the FPO pickup address.");
            return response;
        }

        double pickupLat = pickupShipment.getPickupLatitude();
        double pickupLng = pickupShipment.getPickupLongitude();

        System.out.println("=================================================");
        System.out.println("ORS ROAD ROUTE OPTIMIZATION");
        System.out.println("=================================================");
        System.out.println(
                "Pickup: " + pickupShipment.getPickupAddress());
        System.out.println(
                "Pickup coordinates: "
                        + pickupLat + ", " + pickupLng);
        System.out.println(
                "Destinations: " + validShipments.size());

        System.out.println("-------------------------------------------------");
        System.out.println("DESTINATION COORDINATES");

        for (int i = 0; i < validShipments.size(); i++) {

            FPOShipment s = validShipments.get(i);

            System.out.println(
                    (i + 1)
                            + ". "
                            + s.getDestinationAddress()
                            + " -> "
                            + s.getDestinationLatitude()
                            + ", "
                            + s.getDestinationLongitude());
        }

        /*
         * Point 0 = pickup
         * Point 1..N = destinations
         *
         * Matrix contains ACTUAL ROAD distances from ORS.
         */
        MatrixData matrix =
                buildRoadDistanceMatrix(
                        pickupLat,
                        pickupLng,
                        validShipments);

        if (matrix == null) {
            if (validShipments.size() + 1 > 50) {
                response.setMessage(
                        "This route contains "
                                + (validShipments.size() + 1)
                                + " locations, but the ORS Matrix request is limited to 50 locations. "
                                + "Please split the shipment batch and optimize the routes separately.");
            } else {
                response.setMessage(
                        "OpenRouteService could not calculate the "
                                + "road-distance matrix. Please try again.");
            }
            return response;
        }

        double[][] roadDistances = matrix.distancesKm;
        double[][] roadDurationsSeconds = matrix.durationsSeconds;

        /*
         * Print pickup-to-every-destination distances.
         * This is useful for debugging the exact #1 choice.
         */
        System.out.println("-------------------------------------------------");
        System.out.println("ROAD DISTANCE FROM PICKUP");

        for (int i = 0; i < validShipments.size(); i++) {

            double d = roadDistances[0][i + 1];

            System.out.println(
                    validShipments.get(i).getDestinationAddress()
                            + " = "
                            + (Double.isFinite(d)
                            ? round(d) + " km"
                            : "UNREACHABLE"));
        }

        /*
         * Nearest-neighbour using ONLY ORS road distance.
         */
        List<Integer> routeIndexes = new ArrayList<>();
        Set<Integer> remaining = new HashSet<>();

        for (int i = 0; i < validShipments.size(); i++) {
            remaining.add(i + 1);
        }

        int currentPoint = 0;

        while (!remaining.isEmpty()) {

            int nearestPoint = -1;
            double nearestRoadDistance =
                    Double.POSITIVE_INFINITY;

            for (Integer candidate : remaining) {

                double distance =
                        roadDistances[currentPoint][candidate];

                if (Double.isFinite(distance)
                        && distance < nearestRoadDistance) {

                    nearestRoadDistance = distance;
                    nearestPoint = candidate;
                }
            }

            if (nearestPoint == -1) {
                response.setMessage(
                        "ORS could not find a drivable road between "
                                + "the current location and one or more "
                                + "remaining destinations.");
                return response;
            }

            int shipmentIndex = nearestPoint - 1;

            routeIndexes.add(shipmentIndex);

            System.out.println(
                    "NEXT DESTINATION: "
                            + validShipments
                            .get(shipmentIndex)
                            .getDestinationAddress()
                            + " | ROAD DISTANCE FROM CURRENT = "
                            + round(nearestRoadDistance)
                            + " km");

            remaining.remove(nearestPoint);
            currentPoint = nearestPoint;
        }

        /*
         * Build final response.
         */
        List<RouteStop> stops = new ArrayList<>();

        double totalRoadDistance = 0.0;
        double totalRoadDurationSeconds = 0.0;
        int sequence = 1;
        currentPoint = 0;

        for (Integer shipmentIndex : routeIndexes) {

            FPOShipment shipment =
                    validShipments.get(shipmentIndex);

            int destinationPoint = shipmentIndex + 1;

            double legDistance =
                    roadDistances[currentPoint][destinationPoint];

            double legDurationSeconds =
                    roadDurationsSeconds[currentPoint][destinationPoint];

            if (!Double.isFinite(legDistance)) {

                response.setMessage(
                        "Unable to calculate road distance for "
                                + shipment.getDestinationAddress());
                return response;
            }

            String merchantName = "Merchant";
            Long orderId = null;

            if (shipment.getOrder() != null) {

                orderId =
                        shipment.getOrder().getOrderId();

                if (shipment.getOrder().getMerchant() != null
                        && shipment.getOrder()
                        .getMerchant()
                        .getName() != null) {

                    merchantName =
                            shipment.getOrder()
                                    .getMerchant()
                                    .getName();
                }
            }

            RouteStop stop =
                    new RouteStop(
                            sequence,
                            shipment.getId(),
                            orderId,
                            merchantName,
                            shipment.getDestinationAddress(),
                            round(legDistance),
                            roundMinutes(legDurationSeconds));

            stops.add(stop);
            totalRoadDistance += legDistance;
            if (Double.isFinite(legDurationSeconds)) {
                totalRoadDurationSeconds += legDurationSeconds;
            }

            shipment.setRouteOptimized(true);
            shipment.setRouteSequence(sequence);
            shipment.setRouteOptimizedAt(LocalDateTime.now());

            shipmentRepo.save(shipment);

            currentPoint = destinationPoint;
            sequence++;
        }

        System.out.println("=================================================");
        System.out.println("FINAL ORS ROAD DELIVERY ROUTE");
        System.out.println("=================================================");

        for (RouteStop stop : stops) {

            System.out.println(
                    stop.getSequence()
                            + ". Order #"
                            + stop.getOrderId()
                            + " -> "
                            + stop.getDestinationAddress()
                            + " | ROAD LEG = "
                            + stop.getDistanceFromPreviousKm()
                            + " km | DRIVE TIME = "
                            + stop.getEstimatedDriveTimeMinutes()
                            + " min");
        }

        System.out.println("-------------------------------------------------");
        System.out.println(
                "TOTAL ROAD DISTANCE = "
                        + round(totalRoadDistance)
                        + " km");
        System.out.println(
                "TOTAL ESTIMATED DRIVE TIME = "
                        + roundMinutes(totalRoadDurationSeconds)
                        + " min");
        System.out.println("=================================================");

        response.setStops(stops);
        response.setTotalDistanceKm(round(totalRoadDistance));
        response.setTotalEstimatedTimeMinutes(
                roundMinutes(totalRoadDurationSeconds));

        /*
         * This is ONLY a Google Maps navigation/sharing URL.
         * It does NOT call Google's API and does NOT require
         * Google Maps Platform billing.
         *
         * The order generated by ORS is preserved.
         */
        response.setGoogleMapsUrl(
                buildGoogleMapsUrl(
                        pickupShipment,
                        validShipments,
                        routeIndexes));

        response.setMessage(
                "Road-distance route optimized successfully using "
                        + "OpenRouteService. The driver starts at pickup "
                        + "and at every step visits the nearest remaining "
                        + "destination by driving-road distance.");

        return response;
    }

    /** Returns direct pickup -> destination ROAD distance/time from ORS. */
    public double[] calculateDirectRoadRoute(double pickupLat, double pickupLng,
                                             double destinationLat, double destinationLng) {
        if (orsApiKey == null || orsApiKey.isBlank() || "YOUR_NEW_ORS_KEY_HERE".equals(orsApiKey.trim())) {
            throw new IllegalStateException("OpenRouteService API key is missing.");
        }
        try {
            java.util.Map<String, Object> request = new java.util.HashMap<>();
            request.put("locations", List.of(
                    List.of(pickupLng, pickupLat),
                    List.of(destinationLng, destinationLat)));
            request.put("metrics", List.of("distance", "duration"));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", orsApiKey.trim());
            headers.set("Content-Type", "application/json");
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    URI.create(ORS_MATRIX_URL), HttpMethod.POST, entity, JsonNode.class);
            JsonNode body = response.getBody();
            if (body == null || body.has("error") || !body.has("distances") || !body.has("durations")) {
                throw new IllegalStateException("OpenRouteService did not return a valid road route.");
            }
            double meters = body.get("distances").get(0).get(1).asDouble();
            double seconds = body.get("durations").get(0).get(1).asDouble();
            if (!Double.isFinite(meters) || meters < 0) {
                throw new IllegalStateException("OpenRouteService returned an invalid road distance.");
            }
            return new double[] { meters / 1000.0, seconds / 60.0 };
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate ORS road distance: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // ORS MATRIX
    // =========================================================

    private MatrixData buildRoadDistanceMatrix(
            double pickupLat,
            double pickupLng,
            List<FPOShipment> shipments) {

        /*
         * ORS expects:
         * [longitude, latitude]
         *
         * Point 0 = pickup.
         */
        List<List<Double>> locations = new ArrayList<>();

        locations.add(
                List.of(
                        pickupLng,
                        pickupLat));

        for (FPOShipment shipment : shipments) {

            locations.add(
                    List.of(
                            shipment.getDestinationLongitude(),
                            shipment.getDestinationLatitude()));
        }

        /*
         * ORS free-tier Matrix requests are limited to about 50
         * locations per request. This service intentionally returns
         * a clear failure instead of silently changing the route
         * algorithm or producing a partial matrix.
         */
        if (locations.size() > 50) {
            System.out.println(
                    "ORS MATRIX NOT SENT: "
                            + locations.size()
                            + " locations exceeds the 50-location limit.");
            return null;
        }

        try {

            java.util.Map<String, Object> request =
                    new java.util.HashMap<>();

            request.put("locations", locations);
            request.put(
                    "metrics",
                    List.of("distance", "duration"));

            String json =
                    objectMapper.writeValueAsString(request);

            HttpHeaders headers = new HttpHeaders();

            headers.set(
                    "Authorization",
                    orsApiKey.trim());

            headers.set(
                    "Content-Type",
                    "application/json");

            headers.set(
                    "Accept",
                    "application/json");

            HttpEntity<String> entity =
                    new HttpEntity<>(
                            json,
                            headers);

            System.out.println("ORS MATRIX REQUEST:");
            System.out.println(ORS_MATRIX_URL);

            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(
                            URI.create(ORS_MATRIX_URL),
                            HttpMethod.POST,
                            entity,
                            JsonNode.class);

            JsonNode body = response.getBody();

            if (body == null) {
                System.out.println(
                        "ORS returned an empty response.");
                return null;
            }

            if (body.has("error")) {
                System.out.println(
                        "ORS ERROR: " + body);
                return null;
            }

            JsonNode distancesNode =
                    body.get("distances");

            JsonNode durationsNode =
                    body.get("durations");

            if (distancesNode == null
                    || !distancesNode.isArray()
                    || durationsNode == null
                    || !durationsNode.isArray()) {

                System.out.println(
                        "ORS response does not contain valid "
                                + "distance and duration matrices.");
                System.out.println(body);
                return null;
            }

            int pointCount =
                    shipments.size() + 1;

            if (distancesNode.size() != pointCount
                    || durationsNode.size() != pointCount) {

                System.out.println(
                        "Unexpected ORS matrix size. Expected="
                                + pointCount
                                + " DistanceReceived="
                                + distancesNode.size()
                                + " DurationReceived="
                                + durationsNode.size());
                return null;
            }

            double[][] distanceMatrix =
                    new double[pointCount][pointCount];

            double[][] durationMatrix =
                    new double[pointCount][pointCount];

            for (int i = 0; i < pointCount; i++) {

                JsonNode distanceRow =
                        distancesNode.get(i);
                JsonNode durationRow =
                        durationsNode.get(i);

                if (distanceRow == null
                        || !distanceRow.isArray()
                        || distanceRow.size() != pointCount
                        || durationRow == null
                        || !durationRow.isArray()
                        || durationRow.size() != pointCount) {

                    return null;
                }

                for (int j = 0; j < pointCount; j++) {

                    JsonNode distanceValue =
                            distanceRow.get(j);
                    JsonNode durationValue =
                            durationRow.get(j);

                    if (distanceValue == null
                            || distanceValue.isNull()) {

                        distanceMatrix[i][j] =
                                Double.POSITIVE_INFINITY;

                    } else {

                        /* ORS distance is METERS. */
                        distanceMatrix[i][j] =
                                distanceValue.asDouble() / 1000.0;
                    }

                    if (durationValue == null
                            || durationValue.isNull()) {

                        durationMatrix[i][j] =
                                Double.POSITIVE_INFINITY;

                    } else {

                        /* ORS duration is SECONDS. */
                        durationMatrix[i][j] =
                                durationValue.asDouble();
                    }
                }
            }

            System.out.println(
                    "ORS ROAD DISTANCE + DURATION MATRIX RECEIVED SUCCESSFULLY.");

            return new MatrixData(
                    distanceMatrix,
                    durationMatrix);

        } catch (Exception e) {

            System.out.println(
                    "ORS matrix request failed: "
                            + e.getMessage());

            e.printStackTrace();

            return null;
        }
    }

    // =========================================================
    // COORDINATE VALIDATION
    // =========================================================

    private boolean isKnownMsHospitalAddress(String address) {

        if (address == null || address.isBlank()) {
            return false;
        }

        String normalized = address
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();

        return normalized.contains("ms hospital")
                && normalized.contains("kursi road")
                && normalized.contains("lucknow");
    }

    private boolean hasValidDestinationCoordinates(
            FPOShipment shipment) {

        if (shipment == null) {
            return false;
        }

        return isValidLatitude(
                shipment.getDestinationLatitude())
                && isValidLongitude(
                shipment.getDestinationLongitude());
    }

    private boolean hasValidPickupCoordinates(
            FPOShipment shipment) {

        if (shipment == null) {
            return false;
        }

        return isValidLatitude(
                shipment.getPickupLatitude())
                && isValidLongitude(
                shipment.getPickupLongitude());
    }

    private boolean isValidLatitude(Double value) {

        return value != null
                && !value.isNaN()
                && !value.isInfinite()
                && value >= -90.0
                && value <= 90.0;
    }

    private boolean isValidLongitude(Double value) {

        return value != null
                && !value.isNaN()
                && !value.isInfinite()
                && value >= -180.0
                && value <= 180.0;
    }

    // =========================================================
    // GOOGLE MAPS SHARING / NAVIGATION URL
    // =========================================================

    private String buildGoogleMapsUrl(
            FPOShipment pickupShipment,
            List<FPOShipment> shipments,
            List<Integer> routeIndexes) {

        if (pickupShipment == null
                || routeIndexes == null
                || routeIndexes.isEmpty()) {

            return null;
        }

        /*
         * Google Maps URLs can use addresses OR coordinates.
         *
         * Coordinates are intentionally used here because they
         * point to the same geocoded locations used by ORS.
         *
         * The URL is only for opening/sharing the final route.
         * No Google Maps API call is made.
         */
        StringBuilder url =
                new StringBuilder(
                        "https://www.google.com/maps/dir/?api=1");

        url.append("&origin=")
                .append(
                        coordinate(
                                pickupShipment.getPickupLatitude(),
                                pickupShipment.getPickupLongitude()));

        /*
         * Google Maps requires the final destination separately.
         */
        FPOShipment finalShipment =
                shipments.get(
                        routeIndexes.get(
                                routeIndexes.size() - 1));

        url.append("&destination=")
                .append(
                        coordinate(
                                finalShipment.getDestinationLatitude(),
                                finalShipment.getDestinationLongitude()));

        /*
         * Every earlier stop becomes a waypoint, preserving the
         * exact ORS optimized order.
         *
         * Google Maps supports up to 3 waypoints on mobile browsers
         * and up to 9 otherwise, so this is ideal for the project's
         * small delivery routes.
         */
        if (routeIndexes.size() > 1) {

            StringBuilder waypoints =
                    new StringBuilder();

            for (int i = 0;
                    i < routeIndexes.size() - 1;
                    i++) {

                FPOShipment shipment =
                        shipments.get(
                                routeIndexes.get(i));

                if (waypoints.length() > 0) {
                    waypoints.append("%7C");
                }

                waypoints.append(
                        coordinate(
                                shipment.getDestinationLatitude(),
                                shipment.getDestinationLongitude()));
            }

            if (waypoints.length() > 0) {

                url.append("&waypoints=")
                        .append(waypoints);
            }
        }

        url.append("&travelmode=driving");
        url.append("&dir_action=navigate");

        return url.toString();
    }

    private String coordinate(
            Double latitude,
            Double longitude) {

        return encode(
                String.valueOf(latitude)
                        + ","
                        + String.valueOf(longitude));
    }

    private String encode(String value) {

        if (value == null) {
            return "";
        }

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8);
    }

    private void applyGeocodeMetadata(
            FPOShipment shipment,
            GeocodingService.Coordinates coordinates) {

        shipment.setGeocodeConfidenceScore(
                coordinates.getConfidenceScore());
        shipment.setGeocodeSource(
                coordinates.getSource());
        shipment.setGeocodedAt(LocalDateTime.now());
    }

    private double roundMinutes(double seconds) {

        if (!Double.isFinite(seconds)) {
            return 0.0;
        }

        return Math.round((seconds / 60.0) * 10.0) / 10.0;
    }

    private double round(double value) {

        return Math.round(value * 100.0) / 100.0;
    }
}
