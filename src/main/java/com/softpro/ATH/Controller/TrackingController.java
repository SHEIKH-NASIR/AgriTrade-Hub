package com.softpro.ATH.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Repository.FPOShipmentRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Tracking")
public class TrackingController {

    @Autowired
    private FPOShipmentRepo shipmentRepo;

    /* Existing: shipmentId -> current live location */
    private final Map<Long, LiveLocation> liveLocations =
            new ConcurrentHashMap<>();

    /* Existing: single-shipment driver links */
    private final Map<Long, String> driverTokens =
            new ConcurrentHashMap<>();

    private final Map<String, Long> tokenShipments =
            new ConcurrentHashMap<>();

    /* shipmentId -> FARMER/FPO or DRIVER */
    private final Map<Long, String> trackingSources =
            new ConcurrentHashMap<>();

    /* NEW: one driver route token -> all shipments for that day's route */
    private final Map<String, List<Long>> driverRouteShipments =
            new ConcurrentHashMap<>();

    /* NEW: route token -> existing optimized Google Maps URL */
    private final Map<String, String> driverRouteGoogleMapsUrls =
            new ConcurrentHashMap<>();

    @GetMapping("/{id}")
    public String trackingPage(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        FPOShipment shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found: " + id));

        String role = authorize(session, shipment, true);

        model.addAttribute("shipment", shipment);
        model.addAttribute("driverMode", false);
        model.addAttribute("initialRole", role);
        addLocationModel(model, id);

        return "Tracking/live-tracking";
    }

    /* Existing single-shipment driver link. */
    @GetMapping("/driver/{token}")
    public String driverTrackingPage(
            @PathVariable String token,
            Model model) {

        Long shipmentId = tokenShipments.get(token);

        if (shipmentId == null) {
            throw new IllegalArgumentException(
                    "Invalid or expired driver tracking link.");
        }

        FPOShipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found: " + shipmentId));

        model.addAttribute("shipment", shipment);
        model.addAttribute("driverMode", true);
        model.addAttribute("initialRole", "DRIVER");
        model.addAttribute("driverToken", token);
        addLocationModel(model, shipmentId);

        return "Tracking/live-tracking";
    }

    /* Existing single-shipment driver link creation. */
    @PostMapping("/{id}/driver-link")
    @ResponseBody
    public Map<String, Object> createDriverLink(
            @PathVariable Long id,
            HttpSession session,
            HttpServletRequest request) {

        FPOShipment shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found: " + id));

        String role = authorize(session, shipment, false);

        if (!"FARMER".equals(role) && !"FPO".equals(role)) {
            throw new IllegalArgumentException(
                    "Only Farmer/FPO can create a driver tracking link.");
        }

        String token = driverTokens.computeIfAbsent(
                id, key -> UUID.randomUUID().toString());

        tokenShipments.put(token, id);

        String baseUrl = request.getScheme() + "://" +
                request.getServerName() +
                ((request.getServerPort() == 80 || request.getServerPort() == 443)
                        ? ""
                        : ":" + request.getServerPort());

        String link = baseUrl + "/Tracking/driver/" + token;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("shipmentId", id);
        response.put("trackingLink", link);
        response.put("trackingSource",
                trackingSources.getOrDefault(id, "FARMER/FPO"));

        return response;
    }

    /*
     * NEW: creates ONE tracking link for all stops in the optimized route.
     * The existing Google Maps URL is passed through unchanged.
     */
    @PostMapping("/driver-route-link")
    @ResponseBody
    public Map<String, Object> createDriverRouteLink(
            @RequestParam("shipmentIds") List<Long> shipmentIds,
            @RequestParam(value = "googleMapsUrl", required = false) String googleMapsUrl,
            HttpSession session,
            HttpServletRequest request) {

        if (shipmentIds == null || shipmentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "No shipments were supplied for the driver route.");
        }

        List<Long> cleanIds = new ArrayList<>();

        for (Long id : shipmentIds) {
            if (id == null || cleanIds.contains(id)) {
                continue;
            }

            FPOShipment shipment = shipmentRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Shipment not found: " + id));

            String role = authorize(session, shipment, false);

            if (!"FARMER".equals(role) && !"FPO".equals(role)) {
                throw new IllegalArgumentException(
                        "You are not authorized for shipment " + id);
            }

            cleanIds.add(id);
        }

        if (cleanIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid shipments were supplied for the driver route.");
        }

        String token = UUID.randomUUID().toString();
        driverRouteShipments.put(token, cleanIds);
        driverRouteGoogleMapsUrls.put(token,
                googleMapsUrl == null ? "" : googleMapsUrl);

        String baseUrl = request.getScheme() + "://" +
                request.getServerName() +
                ((request.getServerPort() == 80 || request.getServerPort() == 443)
                        ? ""
                        : ":" + request.getServerPort());

        String link = baseUrl + "/Tracking/driver-route/" + token;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("trackingLink", link);
        response.put("shipmentIds", cleanIds);
        response.put("shipmentCount", cleanIds.size());

        return response;
    }

    /* NEW: one driver page for the entire optimized route. */
    @GetMapping("/driver-route/{token}")
    public String driverRoutePage(
            @PathVariable String token,
            Model model) {

        List<Long> shipmentIds = driverRouteShipments.get(token);

        if (shipmentIds == null || shipmentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid or expired driver route link.");
        }

        List<FPOShipment> shipments = new ArrayList<>();

        for (Long id : shipmentIds) {
            shipmentRepo.findById(id).ifPresent(shipments::add);
        }

        if (shipments.isEmpty()) {
            throw new IllegalArgumentException(
                    "No shipments are available for this driver route.");
        }

        model.addAttribute("shipments", shipments);
        model.addAttribute("driverRouteToken", token);
        model.addAttribute("googleMapsUrl",
                driverRouteGoogleMapsUrls.getOrDefault(token, ""));

        return "Tracking/driver-route";
    }

    /* NEW: driver GPS updates every shipment in today's route at once. */
    @PostMapping("/driver-route/{token}/location")
    @ResponseBody
    public Map<String, Object> updateDriverRouteLocation(
            @PathVariable String token,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        List<Long> shipmentIds = driverRouteShipments.get(token);

        if (shipmentIds == null || shipmentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid or expired driver route link.");
        }

        validateCoordinates(latitude, longitude);

        LocalDateTime now = LocalDateTime.now();
        int updated = 0;

        for (Long id : shipmentIds) {
            if (!shipmentRepo.existsById(id)) {
                continue;
            }

            /* Driver takes over this shipment's live location. */
            trackingSources.put(id, "DRIVER");
            liveLocations.put(id,
                    new LiveLocation(latitude, longitude, now));
            updated++;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("trackingSource", "DRIVER");
        response.put("latitude", latitude);
        response.put("longitude", longitude);
        response.put("lastUpdated", now);
        response.put("updatedShipments", updated);

        return response;
    }

    /* NEW: explicit route takeover if needed. */
    @PostMapping("/driver-route/{token}/start")
    @ResponseBody
    public Map<String, Object> startDriverRouteTracking(
            @PathVariable String token) {

        List<Long> shipmentIds = driverRouteShipments.get(token);

        if (shipmentIds == null || shipmentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid or expired driver route link.");
        }

        for (Long id : shipmentIds) {
            if (shipmentRepo.existsById(id)) {
                trackingSources.put(id, "DRIVER");
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("trackingSource", "DRIVER");
        response.put("shipmentCount", shipmentIds.size());
        return response;
    }

    @GetMapping("/{id}/location")
    @ResponseBody
    public Map<String, Object> getLocation(
            @PathVariable Long id,
            HttpSession session) {

        FPOShipment shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found: " + id));

        authorize(session, shipment, true);

        return locationPayload(shipment);
    }

    /* Existing Farmer/FPO GPS endpoint. Driver takeover wins automatically. */
    @PostMapping("/{id}/location")
    @ResponseBody
    public Map<String, Object> updateLocation(
            @PathVariable Long id,
            @RequestParam double latitude,
            @RequestParam double longitude,
            HttpSession session) {

        FPOShipment shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found: " + id));

        String role = authorize(session, shipment, false);

        if (!"FARMER".equals(role) && !"FPO".equals(role)) {
            throw new IllegalArgumentException(
                    "Only Farmer/FPO can update origin GPS.");
        }

        if ("DRIVER".equals(trackingSources.get(id))) {
            return locationPayload(shipment);
        }

        validateCoordinates(latitude, longitude);

        trackingSources.put(id, "FARMER/FPO");
        liveLocations.put(id, new LiveLocation(
                latitude,
                longitude,
                LocalDateTime.now()));

        return locationPayload(shipment);
    }

    /* Existing single-shipment driver GPS endpoint. */
    @PostMapping("/driver/{token}/location")
    @ResponseBody
    public Map<String, Object> updateDriverLocation(
            @PathVariable String token,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        Long id = tokenShipments.get(token);

        if (id == null) {
            throw new IllegalArgumentException(
                    "Invalid driver tracking link.");
        }

        FPOShipment shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found: " + id));

        validateCoordinates(latitude, longitude);
        trackingSources.put(id, "DRIVER");
        liveLocations.put(id, new LiveLocation(
                latitude,
                longitude,
                LocalDateTime.now()));

        return locationPayload(shipment);
    }

    /* Existing single-shipment driver takeover. */
    @PostMapping("/driver/{token}/start")
    @ResponseBody
    public Map<String, Object> startDriverTracking(
            @PathVariable String token) {

        Long id = tokenShipments.get(token);

        if (id == null) {
            throw new IllegalArgumentException(
                    "Invalid driver tracking link.");
        }

        trackingSources.put(id, "DRIVER");

        FPOShipment shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found: " + id));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("shipmentId", id);
        response.put("trackingSource", "DRIVER");
        response.put("message", "Driver tracking started.");

        return response;
    }

    @PostMapping("/{id}/stop")
    @ResponseBody
    public Map<String, Object> stopTracking(
            @PathVariable Long id,
            HttpSession session) {

        FPOShipment shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found: " + id));

        String role = authorize(session, shipment, false);

        if (!"FARMER".equals(role) && !"FPO".equals(role)) {
            throw new IllegalArgumentException(
                    "Only Farmer/FPO can stop shipment tracking.");
        }

        liveLocations.remove(id);
        trackingSources.remove(id);

        String token = driverTokens.remove(id);
        if (token != null) {
            tokenShipments.remove(token);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("shipmentId", id);
        response.put("message", "Live tracking stopped.");

        return response;
    }

    private void addLocationModel(Model model, Long id) {
        LiveLocation location = liveLocations.get(id);

        if (location != null) {
            model.addAttribute("hasLiveLocation", true);
            model.addAttribute("currentLatitude", location.latitude);
            model.addAttribute("currentLongitude", location.longitude);
            model.addAttribute("lastLocationUpdate", location.lastUpdated);
        } else {
            model.addAttribute("hasLiveLocation", false);
        }

        model.addAttribute(
                "trackingSource",
                trackingSources.getOrDefault(id, "FARMER/FPO"));
    }

    private Map<String, Object> locationPayload(FPOShipment shipment) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("shipmentId", shipment.getId());
        response.put("status", shipment.getStatus());
        response.put("vehicleNumber", shipment.getVehicleNumber());
        response.put("transporterName", shipment.getTransporterName());
        response.put("estimatedDeliveryDate",
                shipment.getEstimatedDeliveryDate());

        response.put("destinationLatitude",
                shipment.getDestinationLatitude());
        response.put("destinationLongitude",
                shipment.getDestinationLongitude());
        response.put("pickupLatitude",
                shipment.getPickupLatitude());
        response.put("pickupLongitude",
                shipment.getPickupLongitude());

        LiveLocation current = liveLocations.get(shipment.getId());

        response.put("trackingSource",
                trackingSources.getOrDefault(
                        shipment.getId(), "FARMER/FPO"));

        if (current != null) {
            response.put("liveTracking", true);
            response.put("latitude", current.latitude);
            response.put("longitude", current.longitude);
            response.put("lastUpdated", current.lastUpdated);
        } else {
            response.put("liveTracking", false);
            response.put("latitude", null);
            response.put("longitude", null);
            response.put("lastUpdated", null);
        }

        return response;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude.");
        }
    }

    private String authorize(
            HttpSession session,
            FPOShipment shipment,
            boolean allowMerchant) {

        Object merchantObject = session.getAttribute("loggedInMerchant");

        if (allowMerchant && merchantObject instanceof Merchant) {
            Merchant merchant = (Merchant) merchantObject;

            if (shipment.getOrder() != null &&
                    shipment.getOrder().getMerchant() != null &&
                    Objects.equals(
                            shipment.getOrder().getMerchant().getId(),
                            merchant.getId())) {
                return "MERCHANT";
            }
        }

        Object farmerObject = session.getAttribute("loggedInFormer");

        if (farmerObject instanceof Formers) {
            Formers farmer = (Formers) farmerObject;

            if (shipment.getFarmer() != null &&
                    shipment.getFarmer().getId() == farmer.getId()) {
                return "FARMER";
            }
        }

        Object fpoIdObject = session.getAttribute("loggedInFPOId");

        if (fpoIdObject != null) {
            try {
                long fpoId = Long.parseLong(fpoIdObject.toString());

                if (shipment.getFpo() != null &&
                        shipment.getFpo().getId() == fpoId) {
                    return "FPO";
                }
            } catch (NumberFormatException ignored) {
                // Invalid session value.
            }
        }

        throw new IllegalArgumentException(
                "You are not authorized to access this shipment.");
    }

    private static class LiveLocation {
        private final double latitude;
        private final double longitude;
        private final LocalDateTime lastUpdated;

        private LiveLocation(
                double latitude,
                double longitude,
                LocalDateTime lastUpdated) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.lastUpdated = lastUpdated;
        }
    }
}
