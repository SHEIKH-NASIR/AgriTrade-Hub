package com.softpro.ATH.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GeocodingService {

    /*
     * Address -> coordinates only.
     *
     * IMPORTANT:
     * This service does NOT calculate route distance.
     * RouteOptimizationService uses OpenRouteService (ORS) for ROAD distance.
     *
     * We request multiple geocoding candidates instead of blindly
     * accepting the first result. This is important for landmarks
     * such as hospitals, malls and localities where the first
     * search result can be incorrect.
     */

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search";

    private static final String PHOTON_URL =
            "https://photon.komoot.io/api/";

    private static final long NOMINATIM_MIN_INTERVAL_MS = 1100L;

    private final RestTemplate restTemplate;

    /*
     * Address-normalized in-memory cache. The cache is shared by all
     * requests handled by this singleton service, so the same address
     * is not sent to a geocoder twice during the application run.
     */
    private final Map<String, Coordinates> geocodeCache =
            new ConcurrentHashMap<>();

    /*
     * Nominatim requires at least 1 request/second. Keep the complete
     * wait + request operation under one lock so concurrent requests
     * cannot accidentally violate the interval.
     */
    private final Object nominatimRateLimitLock = new Object();
    private long lastNominatimRequestAt = 0L;

    public GeocodingService() {
        this.restTemplate = new RestTemplate();
    }

    // =========================================================
    // COORDINATES
    // =========================================================

    public static class Coordinates {

        private final Double latitude;
        private final Double longitude;
        private final double confidenceScore;
        private final String source;

        public Coordinates(
                Double latitude,
                Double longitude) {

            this(latitude, longitude, 0.0, "UNKNOWN");
        }

        public Coordinates(
                Double latitude,
                Double longitude,
                double confidenceScore,
                String source) {

            this.latitude = latitude;
            this.longitude = longitude;
            this.confidenceScore = confidenceScore;
            this.source = source;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public double getConfidenceScore() {
            return confidenceScore;
        }

        public String getSource() {
            return source;
        }
    }

    private static class SelectedCandidate {

        private final JsonNode node;
        private final double score;

        private SelectedCandidate(JsonNode node, double score) {
            this.node = node;
            this.score = score;
        }
    }

    // =========================================================
    // MAIN GEOCODING METHOD
    // =========================================================

    public Coordinates geocode(String address) {
        return geocode(address, false);
    }

    /**
     * Geocodes an address, optionally bypassing the previously stored
     * application coordinates. A force refresh still uses the in-memory
     * cache for the duration of the current run, so the same normalized
     * address is never requested from an external geocoder twice in one run.
     */
    public synchronized Coordinates geocode(
            String address,
            boolean forceRefresh) {

        if (address == null
                || address.trim().isEmpty()) {

            System.out.println(
                    "GEOCODING SKIPPED: empty address"
            );

            return null;
        }

        String original =
                cleanAddress(address);

        if (original.isEmpty()) {
            return null;
        }

        String cacheKey =
                normalizeCacheKey(original);

        // =========================================================
        // KNOWN LOCAL POI OVERRIDE
        // =========================================================
        /*
         * Official MS Hospital address:
         *
         * 421 Bhakhamau, Kursi Road,
         * Lucknow - 226026, Uttar Pradesh
         *
         * IMPORTANT:
         *
         * The previous implementation accidentally used:
         *
         *     26.9586164, 81.0000896
         *
         * which is ALSO the FPO pickup coordinate.
         *
         * Therefore ORS correctly calculated:
         *
         *     0.0 km
         *     0.0 min
         *
         * The MS Hospital POI now uses a different coordinate:
         *
         *     Latitude  = 26.9749346
         *     Longitude = 81.0016159
         *
         * This override prevents ambiguous Nominatim/Photon results
         * from selecting an unrelated hospital.
         */

        String normalizedAddress =
                original.toLowerCase(Locale.ROOT)
                        .replaceAll("\\s+", " ")
                        .trim();

        if (normalizedAddress.contains("ms hospital")
                && normalizedAddress.contains("kursi road")
                && normalizedAddress.contains("lucknow")) {

            System.out.println(
                    "VERIFIED POI OVERRIDE: MS HOSPITAL KURSI ROAD LUCKNOW"
            );

            Coordinates knownCoordinates =
                    new Coordinates(
                            26.9749346,
                            81.0016159,
                            100.0,
                            "VERIFIED_KNOWN_POI"
                    );

            geocodeCache.put(
                    cacheKey,
                    knownCoordinates
            );

            System.out.println(
                    "MS HOSPITAL COORDINATES: "
                            + knownCoordinates.getLatitude()
                            + ", "
                            + knownCoordinates.getLongitude()
            );

            return knownCoordinates;
        }

        Coordinates cached =
                geocodeCache.get(cacheKey);

        if (cached != null) {

            System.out.println(
                    "GEOCODING CACHE HIT"
                            + (forceRefresh
                            ? " (force refresh already performed this run)"
                            : "")
                            + ": "
                            + original
            );

            return cached;
        }

        System.out.println(
                "================================================="
        );

        System.out.println(
                "GEOCODING ADDRESS"
        );

        System.out.println(
                "Original: "
                        + original
        );

        /*
         * Try several increasingly explicit queries.
         */
        List<String> queries =
                buildQueries(original);

        /*
         * -----------------------------------------------------
         * NOMINATIM
         * -----------------------------------------------------
         */

        for (String query : queries) {

            Coordinates result =
                    geocodeWithNominatim(
                            query,
                            original
                    );

            if (result != null) {

                System.out.println(
                        "FINAL GEOCODING RESULT [NOMINATIM]"
                );

                System.out.println(
                        "Address: "
                                + original
                );

                System.out.println(
                        "Query: "
                                + query
                );

                System.out.println(
                        "Latitude: "
                                + result.getLatitude()
                );

                System.out.println(
                        "Longitude: "
                                + result.getLongitude()
                );

                System.out.println(
                        "================================================="
                );

                geocodeCache.put(
                        cacheKey,
                        result
                );

                return result;
            }
        }

        /*
         * -----------------------------------------------------
         * PHOTON FALLBACK
         * -----------------------------------------------------
         */

        for (String query : queries) {

            Coordinates result =
                    geocodeWithPhoton(
                            query,
                            original
                    );

            if (result != null) {

                System.out.println(
                        "FINAL GEOCODING RESULT [PHOTON]"
                );

                System.out.println(
                        "Address: "
                                + original
                );

                System.out.println(
                        "Query: "
                                + query
                );

                System.out.println(
                        "Latitude: "
                                + result.getLatitude()
                );

                System.out.println(
                        "Longitude: "
                                + result.getLongitude()
                );

                System.out.println(
                        "================================================="
                );

                geocodeCache.put(
                        cacheKey,
                        result
                );

                return result;
            }
        }

        System.out.println(
                "GEOCODING FAILED: "
                        + original
        );

        System.out.println(
                "================================================="
        );

        return null;
    }

    // =========================================================
    // BUILD SEARCH QUERIES
    // =========================================================

    private List<String> buildQueries(
            String address) {

        Set<String> queries =
                new LinkedHashSet<>();

        String normalized =
                address
                        .replaceAll("\\s+", " ")
                        .trim();

        String lower =
                normalized.toLowerCase(
                        Locale.ROOT
                );

        boolean hasLucknow =
                lower.contains("lucknow");

        boolean hasIndia =
                lower.contains("india");

        boolean hasUP =
                lower.contains("uttar pradesh")
                        || lower.matches(
                        ".*\\bup\\b.*"
                );

        /*
         * Original first.
         */
        queries.add(
                normalized
        );

        /*
         * Explicit Lucknow.
         */
        if (!hasLucknow) {

            queries.add(
                    normalized
                            + ", Lucknow"
            );
        }

        /*
         * Explicit Uttar Pradesh.
         */
        if (!hasUP) {

            queries.add(
                    normalized
                            + ", Lucknow, Uttar Pradesh"
            );
        }

        /*
         * Full India context.
         */
        if (!hasIndia) {

            queries.add(
                    normalized
                            + ", Lucknow, Uttar Pradesh, India"
            );
        }

        /*
         * If Lucknow was already present but UP/India were
         * missing, still create a fully explicit query.
         */
        if (hasLucknow
                && (!hasUP || !hasIndia)) {

            queries.add(
                    normalized
                            + ", Uttar Pradesh, India"
            );
        }

        return new ArrayList<>(
                queries
        );
    }

    // =========================================================
    // NOMINATIM
    // =========================================================

    private Coordinates geocodeWithNominatim(
            String query,
            String originalAddress) {

        try {

            String encoded =
                    URLEncoder.encode(
                            query,
                            StandardCharsets.UTF_8
                    );

            /*
             * Ask for several candidates.
             *
             * limit=5 is intentional.
             * We then score the candidates instead of accepting
             * candidate #1 blindly.
             */
            String url =
                    NOMINATIM_URL
                            + "?q="
                            + encoded
                            + "&format=json"
                            + "&limit=5"
                            + "&addressdetails=1"
                            + "&countrycodes=in";

            HttpHeaders headers =
                    new HttpHeaders();

            headers.set(
                    "User-Agent",
                    "ATH-Route-Optimization/1.0 "
                            + "(Agritrade Hub logistics)"
            );

            headers.set(
                    "Accept",
                    "application/json"
            );

            HttpEntity<Void> entity =
                    new HttpEntity<>(
                            headers
                    );

            waitForNominatimRateLimit();

            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(
                            URI.create(url),
                            HttpMethod.GET,
                            entity,
                            JsonNode.class
                    );

            JsonNode body =
                    response.getBody();

            if (body == null
                    || !body.isArray()
                    || body.isEmpty()) {

                return null;
            }

            SelectedCandidate selected =
                    chooseBestNominatimResult(
                            body,
                            originalAddress
                    );

            if (selected == null) {
                return null;
            }

            JsonNode best =
                    selected.node;

            JsonNode latNode =
                    best.get("lat");

            JsonNode lonNode =
                    best.get("lon");

            if (latNode == null
                    || lonNode == null) {

                return null;
            }

            Coordinates coordinates =
                    parseCoordinates(
                            latNode.asText(),
                            lonNode.asText(),
                            selected.score,
                            "NOMINATIM"
                    );

            if (coordinates != null) {

                System.out.println(
                        "NOMINATIM SELECTED RESULT:"
                );

                System.out.println(
                        "Display name: "
                                + best.path(
                                "display_name"
                        ).asText()
                );

                System.out.println(
                        "Type: "
                                + best.path(
                                "type"
                        ).asText()
                );

                System.out.println(
                        "Coordinates: "
                                + coordinates.getLatitude()
                                + ", "
                                + coordinates.getLongitude()
                );
            }

            return coordinates;

        } catch (Exception e) {

            System.out.println(
                    "Nominatim error for '"
                            + query
                            + "': "
                            + e.getMessage()
            );

            return null;
        }
    }

    // =========================================================
    // CHOOSE BEST NOMINATIM RESULT
    // =========================================================

    private SelectedCandidate chooseBestNominatimResult(
            JsonNode results,
            String originalAddress) {

        if (results == null
                || !results.isArray()
                || results.isEmpty()) {

            return null;
        }

        String original =
                originalAddress.toLowerCase(
                        Locale.ROOT
                );

        JsonNode best =
                null;

        double bestScore =
                Double.NEGATIVE_INFINITY;

        for (JsonNode result :
                results) {

            if (result == null) {
                continue;
            }

            JsonNode latNode =
                    result.get("lat");

            JsonNode lonNode =
                    result.get("lon");

            if (latNode == null
                    || lonNode == null) {

                continue;
            }

            String display =
                    result.path(
                            "display_name"
                    )
                    .asText("")
                    .toLowerCase(
                            Locale.ROOT
                    );

            String type =
                    result.path(
                            "type"
                    )
                    .asText("")
                    .toLowerCase(
                            Locale.ROOT
                    );

            String category =
                    result.path(
                            "category"
                    )
                    .asText("")
                    .toLowerCase(
                            Locale.ROOT
                    );

            String combined =
                    display
                            + " "
                            + type
                            + " "
                            + category;

            double score =
                    0.0;

            /*
             * Strong preference for Lucknow.
             */
            if (combined.contains(
                    "lucknow"
            )) {

                score += 50;
            }

            /*
             * Strong preference for Uttar Pradesh.
             */
            if (combined.contains(
                    "uttar pradesh"
            )) {

                score += 25;
            }

            /*
             * Match important words from the original address.
             */
            String[] words =
                    original
                            .replaceAll(
                                    "[^a-z0-9 ]",
                                    " "
                            )
                            .split(
                                    "\\s+"
                            );

            for (String word :
                    words) {

                if (word.length() < 3) {
                    continue;
                }

                /*
                 * Ignore generic geographic words.
                 */
                if (word.equals("the")
                        || word.equals("road")
                        || word.equals("lucknow")
                        || word.equals("india")
                        || word.equals("uttar")
                        || word.equals("pradesh")) {

                    continue;
                }

                if (combined.contains(word)) {

                    score += 8;
                }
            }

            /*
             * Prefer useful POI/address types over a generic
             * administrative result.
             */
            if (type.equals("hospital")
                    || type.equals("clinic")
                    || type.equals("mall")
                    || type.equals("school")
                    || type.equals("university")
                    || type.equals("college")
                    || type.equals("commercial")
                    || type.equals("retail")
                    || type.equals("road")
                    || type.equals("residential")) {

                score += 10;
            }

            /*
             * Avoid results that are clearly outside Lucknow.
             */
            if (!combined.contains("lucknow")
                    && original.contains("lucknow")) {

                score -= 100;
            }

            System.out.println(
                    "NOMINATIM CANDIDATE:"
                            + " score="
                            + score
                            + " | "
                            + result.path(
                            "display_name"
                    ).asText()
            );

            if (score > bestScore) {

                bestScore =
                        score;

                best =
                        result;
            }
        }

        return best == null
                ? null
                : new SelectedCandidate(
                best,
                bestScore
        );
    }

    // =========================================================
    // PHOTON
    // =========================================================

    private Coordinates geocodeWithPhoton(
            String query,
            String originalAddress) {

        try {

            String encoded =
                    URLEncoder.encode(
                            query,
                            StandardCharsets.UTF_8
                    );

            String url =
                    PHOTON_URL
                            + "?q="
                            + encoded
                            + "&limit=5";

            HttpHeaders headers =
                    new HttpHeaders();

            headers.set(
                    "User-Agent",
                    "ATH-Route-Optimization/1.0 "
                            + "(Agritrade Hub logistics)"
            );

            headers.set(
                    "Accept",
                    "application/json"
            );

            HttpEntity<Void> entity =
                    new HttpEntity<>(
                            headers
                    );

            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(
                            URI.create(url),
                            HttpMethod.GET,
                            entity,
                            JsonNode.class
                    );

            JsonNode body =
                    response.getBody();

            if (body == null) {
                return null;
            }

            JsonNode features =
                    body.get(
                            "features"
                    );

            if (features == null
                    || !features.isArray()
                    || features.isEmpty()) {

                return null;
            }

            SelectedCandidate selected =
                    chooseBestPhotonResult(
                            features,
                            originalAddress
                    );

            if (selected == null) {
                return null;
            }

            JsonNode best =
                    selected.node;

            JsonNode geometry =
                    best.get(
                            "geometry"
                    );

            if (geometry == null) {
                return null;
            }

            JsonNode coordinates =
                    geometry.get(
                            "coordinates"
                    );

            if (coordinates == null
                    || !coordinates.isArray()
                    || coordinates.size() < 2) {

                return null;
            }

            /*
             * GeoJSON:
             *
             * [longitude, latitude]
             */
            double longitude =
                    coordinates
                            .get(0)
                            .asDouble();

            double latitude =
                    coordinates
                            .get(1)
                            .asDouble();

            Coordinates result =
                    validateCoordinates(
                            latitude,
                            longitude,
                            selected.score,
                            "PHOTON"
                    );

            if (result != null) {

                System.out.println(
                        "PHOTON SELECTED RESULT:"
                );

                System.out.println(
                        "Coordinates: "
                                + latitude
                                + ", "
                                + longitude
                );
            }

            return result;

        } catch (Exception e) {

            System.out.println(
                    "Photon error for '"
                            + query
                            + "': "
                            + e.getMessage()
            );

            return null;
        }
    }

    // =========================================================
    // CHOOSE BEST PHOTON RESULT
    // =========================================================

    private SelectedCandidate chooseBestPhotonResult(
            JsonNode features,
            String originalAddress) {

        JsonNode best =
                null;

        double bestScore =
                Double.NEGATIVE_INFINITY;

        String original =
                originalAddress.toLowerCase(
                        Locale.ROOT
                );

        for (JsonNode feature :
                features) {

            if (feature == null) {
                continue;
            }

            JsonNode properties =
                    feature.get(
                            "properties"
                    );

            if (properties == null) {
                continue;
            }

            String name =
                    properties.path(
                            "name"
                    ).asText("")
                    .toLowerCase(
                            Locale.ROOT
                    );

            String city =
                    properties.path(
                            "city"
                    ).asText("")
                    .toLowerCase(
                            Locale.ROOT
                    );

            String state =
                    properties.path(
                            "state"
                    ).asText("")
                    .toLowerCase(
                            Locale.ROOT
                    );

            String street =
                    properties.path(
                            "street"
                    ).asText("")
                    .toLowerCase(
                            Locale.ROOT
                    );

            String combined =
                    name
                            + " "
                            + city
                            + " "
                            + state
                            + " "
                            + street;

            double score =
                    0.0;

            if (city.contains("lucknow")) {
                score += 50;
            }

            if (state.contains(
                    "uttar pradesh"
            )) {

                score += 25;
            }

            String[] words =
                    original
                            .replaceAll(
                                    "[^a-z0-9 ]",
                                    " "
                            )
                            .split(
                                    "\\s+"
                            );

            for (String word :
                    words) {

                if (word.length() < 3) {
                    continue;
                }

                if (word.equals("the")
                        || word.equals("road")
                        || word.equals("lucknow")
                        || word.equals("india")
                        || word.equals("uttar")
                        || word.equals("pradesh")) {

                    continue;
                }

                if (combined.contains(word)) {
                    score += 8;
                }
            }

            System.out.println(
                    "PHOTON CANDIDATE:"
                            + " score="
                            + score
                            + " | name="
                            + name
                            + " | city="
                            + city
                            + " | street="
                            + street
            );

            if (score > bestScore) {

                bestScore =
                        score;

                best =
                        feature;
            }
        }

        return best == null
                ? null
                : new SelectedCandidate(
                best,
                bestScore
        );
    }

    // =========================================================
    // PARSE
    // =========================================================

    private Coordinates parseCoordinates(
            String latitudeText,
            String longitudeText,
            double confidenceScore,
            String source) {

        try {

            double latitude =
                    Double.parseDouble(
                            latitudeText
                    );

            double longitude =
                    Double.parseDouble(
                            longitudeText
                    );

            return validateCoordinates(
                    latitude,
                    longitude,
                    confidenceScore,
                    source
            );

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // VALIDATE
    // =========================================================

    private Coordinates validateCoordinates(
            double latitude,
            double longitude,
            double confidenceScore,
            String source) {

        if (Double.isNaN(latitude)
                || Double.isNaN(longitude)
                || Double.isInfinite(latitude)
                || Double.isInfinite(longitude)) {

            return null;
        }

        if (latitude < -90.0
                || latitude > 90.0
                || longitude < -180.0
                || longitude > 180.0) {

            return null;
        }

        /*
         * India bounding check.
         */
        if (latitude < 6.0
                || latitude > 38.0
                || longitude < 68.0
                || longitude > 98.0) {

            System.out.println(
                    "Rejected non-India coordinate: "
                            + latitude
                            + ", "
                            + longitude
            );

            return null;
        }

        return new Coordinates(
                latitude,
                longitude,
                confidenceScore,
                source
        );
    }

    private void waitForNominatimRateLimit()
            throws InterruptedException {

        synchronized (nominatimRateLimitLock) {

            long now =
                    System.currentTimeMillis();

            long waitMs =
                    NOMINATIM_MIN_INTERVAL_MS
                            - (now - lastNominatimRequestAt);

            if (waitMs > 0) {
                Thread.sleep(waitMs);
            }

            lastNominatimRequestAt =
                    System.currentTimeMillis();
        }
    }

    private String normalizeCacheKey(
            String address) {

        return cleanAddress(address)
                .toLowerCase(Locale.ROOT);
    }

    // =========================================================
    // CLEAN ADDRESS
    // =========================================================

    private String cleanAddress(
            String address) {

        return address
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }
}