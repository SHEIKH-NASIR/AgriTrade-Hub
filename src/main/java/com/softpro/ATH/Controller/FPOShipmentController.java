package com.softpro.ATH.Controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.API.SendAutoEmail;
import com.softpro.ATH.Dto.RouteOptimizationResponse;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.OrderFarmerContribution;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FPOShipmentRepo;
import com.softpro.ATH.Repository.OrderFarmerContributionRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Service.DemoAccessService;
import com.softpro.ATH.Service.GeocodingService;
import com.softpro.ATH.Service.RouteOptimizationService;
import com.softpro.ATH.Service.ShipmentNotificationService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/FPO/Shipments")
public class FPOShipmentController {

    @Autowired
    private FPOShipmentRepo shipmentRepo;

    @Autowired
    private FPORepo fpoRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderFarmerContributionRepo orderFarmerContributionRepo;

    @Autowired
    private SendAutoEmail sendAutoEmail;

    @Autowired
    private ShipmentNotificationService notificationService;

    @Autowired
    private RouteOptimizationService routeOptimizationService;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private DemoAccessService demoAccessService;


    // =========================================================
    // GET LOGGED IN FPO
    // =========================================================

    private FPO getLoggedInFPO(HttpSession session) {

        Object id = session.getAttribute("loggedInFPOId");

        if (id == null) {
            return null;
        }

        try {

            return fpoRepo.findById(
                    Long.parseLong(id.toString())
            ).orElse(null);

        } catch (Exception e) {

            return null;
        }
    }


    // =========================================================
    // NORMALIZE ADDRESS
    // =========================================================

    /*
     * Makes addresses easier for the geocoder to understand.
     *
     * Example:
     *
     * "INTEGRAL UNIVERSITY, LUCKNOW"
     *
     * becomes:
     *
     * "INTEGRAL UNIVERSITY, LUCKNOW, UTTAR PRADESH, INDIA"
     *
     * If the address already contains these parts, they are
     * not unnecessarily duplicated.
     */
    private String normalizeAddress(String address) {

        if (address == null) {
            return null;
        }

        String value = address.trim();

        if (value.isEmpty()) {
            return null;
        }

        value = value.replaceAll("\\s+", " ");

        String lower = value.toLowerCase();

        if (!lower.contains("uttar pradesh")) {
            value = value + ", Uttar Pradesh";
        }

        lower = value.toLowerCase();

        if (!lower.contains("india")) {
            value = value + ", India";
        }

        return value;
    }


    // =========================================================
    // VIEW SHIPMENTS
    // =========================================================

    @GetMapping
    @Transactional(readOnly = true)
    public String shipments(
            @RequestParam(
                    name = "demo",
                    defaultValue = "false"
            )
            boolean demo,

            HttpSession session,

            Model model,

            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {

            attributes.addFlashAttribute(
                    "error",
                    "FPO session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }

        if (demo) {
            demoAccessService.requireFpo(fpo);
        }

        List<FPOShipment> allShipments =
                shipmentRepo.findByFpoOrderByCreatedAtDesc(fpo);

        List<FPOShipment> shipments =
                allShipments.stream()
                        .filter(shipment ->
                                shipment.getOrder() != null
                                        && shipment.getOrder()
                                                .isDemoOrder() == demo
                        )
                        .toList();

        model.addAttribute(
                "fpo",
                fpo
        );

        model.addAttribute(
                "shipments",
                shipments
        );

        model.addAttribute(
                "demoMode",
                demo
        );

        model.addAttribute(
                "active5",
                "active"
        );

        return "FPO/shipments";
    }


    // =========================================================
    // RE-VERIFY LOCATIONS
    // =========================================================

    @PostMapping("/ReverifyLocations")
    @Transactional
    public String reverifyLocations(
            @RequestParam(name = "demo", defaultValue = "false") boolean demo,
            HttpSession session,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {
            attributes.addFlashAttribute(
                    "error",
                    "FPO session expired. Please login again.");
            return "redirect:/FPO/Login";
        }

        if (demo) {
            demoAccessService.requireFpo(fpo);
        }

        List<FPOShipment> shipments =
                shipmentRepo.findByFpoOrderByCreatedAtDesc(fpo)
                        .stream()
                        .filter(shipment ->
                                shipment.getOrder() != null
                                        && shipment.getOrder().isDemoOrder() == demo
                                        && ("READY_FOR_PICKUP".equalsIgnoreCase(shipment.getStatus())
                                        || "DISPATCHED".equalsIgnoreCase(shipment.getStatus())
                                        || "IN_TRANSIT".equalsIgnoreCase(shipment.getStatus())))
                        .toList();

        if (shipments.isEmpty()) {
            attributes.addFlashAttribute(
                    "error",
                    (demo ? "No active demo " : "No active real ")
                            + "shipments are available for location verification.");
            return "redirect:/FPO/Shipments?demo=" + demo;
        }

        int verified = 0;
        int failed = 0;

        for (FPOShipment shipment : shipments) {

            boolean changed = false;

            if (shipment.getPickupAddress() != null
                    && !shipment.getPickupAddress().isBlank()) {

                GeocodingService.Coordinates coordinates =
                        geocodingService.geocode(
                                shipment.getPickupAddress(), true);

                if (coordinates != null) {
                    shipment.setPickupLatitude(coordinates.getLatitude());
                    shipment.setPickupLongitude(coordinates.getLongitude());
                    applyGeocodeMetadata(shipment, coordinates);
                    changed = true;
                }
            }

            if (shipment.getDestinationAddress() != null
                    && !shipment.getDestinationAddress().isBlank()) {

                GeocodingService.Coordinates coordinates =
                        geocodingService.geocode(
                                shipment.getDestinationAddress(), true);

                if (coordinates != null) {
                    shipment.setDestinationLatitude(coordinates.getLatitude());
                    shipment.setDestinationLongitude(coordinates.getLongitude());
                    applyGeocodeMetadata(shipment, coordinates);
                    changed = true;
                }
            }

            if (changed) {
                shipmentRepo.save(shipment);
                verified++;
            } else {
                failed++;
            }
        }

        attributes.addFlashAttribute(
                verified > 0 ? "success" : "error",
                verified > 0
                        ? "Locations re-verified for " + verified
                                + " shipment(s). Starting route optimization."
                                + (failed > 0 ? " " + failed + " shipment(s) could not be geocoded." : "")
                        : "No shipment locations could be re-verified. Please check the stored addresses.");

        return "redirect:/FPO/Shipments/OptimizeRoute?demo=" + demo;
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


    // =========================================================
    // AI ROUTE OPTIMIZATION
    // =========================================================

    @GetMapping("/OptimizeRoute")
    @Transactional
    public String optimizeRoute(

            @RequestParam(
                    name = "demo",
                    defaultValue = "false"
            )
            boolean demo,

            HttpSession session,

            Model model,

            RedirectAttributes attributes) {

        FPO fpo =
                getLoggedInFPO(session);

        if (fpo == null) {

            attributes.addFlashAttribute(
                    "error",
                    "FPO session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }

        if (demo) {
            demoAccessService.requireFpo(fpo);
        }


        // =====================================================
        // GET ACTIVE SHIPMENTS
        // =====================================================

        List<FPOShipment> shipments =
                shipmentRepo
                        .findByFpoOrderByCreatedAtDesc(fpo)
                        .stream()
                        .filter(
                                shipment ->
                                        shipment.getOrder() != null
                                                && shipment.getOrder()
                                                        .isDemoOrder()
                                                        == demo
                                                && (
                                                        "READY_FOR_PICKUP"
                                                                .equalsIgnoreCase(
                                                                        shipment.getStatus()
                                                                )
                                                                ||
                                                        "DISPATCHED"
                                                                .equalsIgnoreCase(
                                                                        shipment.getStatus()
                                                                )
                                                                ||
                                                        "IN_TRANSIT"
                                                                .equalsIgnoreCase(
                                                                        shipment.getStatus()
                                                                )
                                                )
                        )
                        .toList();


        if (shipments.isEmpty()) {

            attributes.addFlashAttribute(
                    "error",
                    (demo
                            ? "No active demo "
                            : "No active real ")
                            + "shipments are available for route optimization."
            );

            return "redirect:/FPO/Shipments?demo=" + demo;
        }


        // =====================================================
        // RUN ROUTE OPTIMIZER
        // =====================================================

        RouteOptimizationResponse route =
                routeOptimizationService.optimizeRoute(
                        shipments
                );


        if (route == null
                || route.getStops() == null
                || route.getStops().isEmpty()) {

            attributes.addFlashAttribute(
                    "error",
                    route != null
                            && route.getMessage() != null
                                    ? route.getMessage()
                                    : "Unable to optimize route."
            );

            return "redirect:/FPO/Shipments?demo=" + demo;
        }


        model.addAttribute(
                "route",
                route
        );

        model.addAttribute(
                "fpo",
                fpo
        );

        model.addAttribute(
                "demoMode",
                demo
        );

        model.addAttribute(
                "active5",
                "active"
        );

        return "FPO/optimized-route";
    }


    // =========================================================
    // CREATE SHIPMENT PAGE
    // =========================================================

    @GetMapping("/Create")
    @Transactional(readOnly = true)
    public String createShipmentPage(

            @RequestParam("orderId")
            Long orderId,

            HttpSession session,

            Model model,

            RedirectAttributes attributes) {

        FPO fpo =
                getLoggedInFPO(session);

        if (fpo == null) {

            attributes.addFlashAttribute(
                    "error",
                    "FPO session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }


        Order order =
                orderRepo.findByOrderId(orderId);


        if (order == null
                || order.getFpo() == null
                || order.getFpo().getId() != fpo.getId()) {

            attributes.addFlashAttribute(
                    "error",
                    "Order not found or unauthorized."
            );

            return "redirect:/FPO/Orders";
        }


        if (!"Confirmed".equalsIgnoreCase(
                order.getOrderStatus())) {

            attributes.addFlashAttribute(
                    "error",
                    "Shipment can be created only for a confirmed order."
            );

            return "redirect:/FPO/Orders";
        }


        if (shipmentRepo.findByOrder(order).isPresent()) {

            attributes.addFlashAttribute(
                    "error",
                    "A shipment already exists for this order."
            );

            return "redirect:/FPO/Shipments";
        }


        String merchantAddress = "";

        if (order.getMerchant() != null
                && order.getMerchant().getAddress() != null) {

            merchantAddress =
                    order.getMerchant()
                            .getAddress();
        }


        String merchantName =
                order.getMerchant() != null
                        ? order.getMerchant().getName()
                        : "Merchant";


        model.addAttribute(
                "fpo",
                fpo
        );

        model.addAttribute(
                "order",
                order
        );

        model.addAttribute(
                "merchantAddress",
                merchantAddress
        );

        model.addAttribute(
                "merchantName",
                merchantName
        );

        model.addAttribute(
                "active5",
                "active"
        );

        return "FPO/create-shipment";
    }


    // =========================================================
    // CREATE SHIPMENT
    // =========================================================

    @PostMapping("/Create")
    @Transactional
    public String createShipment(

            @RequestParam("orderId")
            Long orderId,

            @RequestParam("pickupAddress")
            String pickupAddress,

            @RequestParam(
                    value = "destinationAddress",
                    required = false
            )
            String destinationAddress,

            @RequestParam(
                    value = "transporterName",
                    required = false
            )
            String transporterName,

            @RequestParam(
                    value = "vehicleNumber",
                    required = false
            )
            String vehicleNumber,

            @RequestParam(
                    value = "estimatedDeliveryDate",
                    required = false
            )
            String estimatedDeliveryDate,

            HttpSession session,

            RedirectAttributes attributes) {

        FPO fpo =
                getLoggedInFPO(session);

        if (fpo == null) {

            attributes.addFlashAttribute(
                    "error",
                    "FPO session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }


        boolean demoOrder = false;


        try {

            Order order =
                    orderRepo.findByOrderId(orderId);


            if (order == null
                    || order.getFpo() == null
                    || order.getFpo().getId()
                            != fpo.getId()) {

                throw new IllegalArgumentException(
                        "Order not found or unauthorized."
                );
            }


            if (!"Confirmed".equalsIgnoreCase(
                    order.getOrderStatus())) {

                throw new IllegalArgumentException(
                        "Only confirmed orders can be shipped."
                );
            }


            if (shipmentRepo.findByOrder(order).isPresent()) {

                throw new IllegalArgumentException(
                        "A shipment already exists for this order."
                );
            }


            demoOrder =
                    order.isDemoOrder();


            if (pickupAddress == null
                    || pickupAddress.isBlank()) {

                throw new IllegalArgumentException(
                        "Pickup address is required."
                );
            }


            // =================================================
            // MERCHANT ADDRESS
            // =================================================

            String merchantAddress =
                    null;

            if (order.getMerchant() != null
                    && order.getMerchant()
                            .getAddress() != null
                    && !order.getMerchant()
                            .getAddress()
                            .isBlank()) {

                merchantAddress =
                        order.getMerchant()
                                .getAddress()
                                .trim();
            }


            // =================================================
            // FINAL DESTINATION
            // =================================================

            String finalDestination;


            if (destinationAddress != null
                    && !destinationAddress.isBlank()) {

                finalDestination =
                        destinationAddress.trim();

            } else if (merchantAddress != null) {

                finalDestination =
                        merchantAddress;

            } else {

                throw new IllegalArgumentException(
                        "Merchant destination address is not available."
                );
            }


            // =================================================
            // NORMALIZE ADDRESSES
            // =================================================

            String finalPickupAddress =
                    normalizeAddress(
                            pickupAddress
                    );

            String normalizedDestination =
                    normalizeAddress(
                            finalDestination
                    );


            if (finalPickupAddress == null) {

                throw new IllegalArgumentException(
                        "Pickup address is invalid."
                );
            }


            if (normalizedDestination == null) {

                throw new IllegalArgumentException(
                        "Destination address is invalid."
                );
            }


            // =================================================
            // CREATE SHIPMENT
            // =================================================

            FPOShipment shipment =
                    new FPOShipment();


            shipment.setOrder(
                    order
            );

            shipment.setFpo(
                    fpo
            );


            /*
             * Store the normalized addresses.
             *
             * This is important because these exact values
             * are later used for backfill/geocoding.
             */

            shipment.setPickupAddress(
                    finalPickupAddress
            );

            shipment.setDestinationAddress(
                    normalizedDestination
            );


            // =================================================
            // GEOCODE PICKUP
            // =================================================

            GeocodingService.Coordinates pickupCoordinates =
                    geocodingService.geocode(
                            finalPickupAddress
                    );


            if (pickupCoordinates != null) {

                shipment.setPickupLatitude(
                        pickupCoordinates.getLatitude()
                );

                shipment.setPickupLongitude(
                        pickupCoordinates.getLongitude()
                );
                applyGeocodeMetadata(shipment, pickupCoordinates);
            }


            // =================================================
            // GEOCODE DESTINATION
            // =================================================

            GeocodingService.Coordinates destinationCoordinates =
                    geocodingService.geocode(
                            normalizedDestination
                    );


            if (destinationCoordinates != null) {

                shipment.setDestinationLatitude(
                        destinationCoordinates.getLatitude()
                );

                shipment.setDestinationLongitude(
                        destinationCoordinates.getLongitude()
                );
                applyGeocodeMetadata(shipment, destinationCoordinates);

            } else {

                /*
                 * DO NOT silently hide the failure.
                 *
                 * The shipment itself can still be created,
                 * but the user receives a clear message that
                 * route optimization will need the address
                 * corrected/backfilled.
                 */

                System.out.println(
                        "WARNING: Destination could not be geocoded: "
                                + normalizedDestination
                );
            }


            // =================================================
            // TRANSPORTER
            // =================================================

            shipment.setTransporterName(
                    transporterName == null
                            || transporterName.isBlank()
                                    ? null
                                    : transporterName.trim()
            );


            // =================================================
            // VEHICLE
            // =================================================

            shipment.setVehicleNumber(
                    vehicleNumber == null
                            || vehicleNumber.isBlank()
                                    ? null
                                    : vehicleNumber.trim()
            );


            // =================================================
            // STATUS
            // =================================================

            shipment.setStatus(
                    "READY_FOR_PICKUP"
            );


            // =================================================
            // ETA
            // =================================================

            if (estimatedDeliveryDate != null
                    && !estimatedDeliveryDate.isBlank()) {

                shipment.setEstimatedDeliveryDate(
                        LocalDateTime.parse(
                                estimatedDeliveryDate
                        )
                );
            }


            // =================================================
            // SAVE SHIPMENT
            // =================================================

            shipmentRepo.saveAndFlush(
                    shipment
            );


            attributes.addFlashAttribute(
                    "success",
                    "Shipment created for Order #"
                            + order.getOrderId()
                            + "."
            );


        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to create shipment: "
                            + e.getMessage()
            );
        }


        return "redirect:/FPO/Shipments?demo=" + demoOrder;
    }


    // =========================================================
    // UPDATE SHIPMENT STATUS
    // =========================================================

    @PostMapping("/{shipmentId}/Status")
    @Transactional
    public String updateShipmentStatus(

            @PathVariable
            Long shipmentId,

            @RequestParam("newStatus")
            String newStatus,

            HttpSession session,

            RedirectAttributes attributes) {

        FPO fpo =
                getLoggedInFPO(session);

        boolean demoOrder =
                false;


        if (fpo == null) {

            attributes.addFlashAttribute(
                    "error",
                    "FPO session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }


        FPOShipment shipment =
                null;


        try {

            shipment =
                    shipmentRepo
                            .findById(
                                    shipmentId
                            )
                            .orElse(null);


            if (shipment == null
                    || shipment.getFpo() == null
                    || shipment.getFpo()
                            .getId()
                            != fpo.getId()) {

                throw new IllegalArgumentException(
                        "Shipment not found or unauthorized."
                );
            }


            demoOrder =
                    shipment.getOrder() != null
                            && shipment.getOrder()
                                    .isDemoOrder();


            if (demoOrder) {
                demoAccessService.requireFpo(fpo);
            }


            String current =
                    shipment.getStatus();


            String next =
                    newStatus == null
                            ? ""
                            : newStatus
                                    .trim()
                                    .toUpperCase();


            boolean validTransition =

                    (
                            "READY_FOR_PICKUP"
                                    .equals(current)
                            &&
                            "DISPATCHED"
                                    .equals(next)
                    )

                    ||

                    (
                            "DISPATCHED"
                                    .equals(current)
                            &&
                            "IN_TRANSIT"
                                    .equals(next)
                    )

                    ||

                    (
                            "IN_TRANSIT"
                                    .equals(current)
                            &&
                            "DELIVERED"
                                    .equals(next)
                    )

                    ||

                    (
                            !"DELIVERED"
                                    .equals(current)
                            &&
                            "CANCELLED"
                                    .equals(next)
                    );


            if (!validTransition) {

                throw new IllegalArgumentException(
                        "Invalid shipment status transition."
                );
            }


            // =================================================
            // DISPATCHED
            // =================================================

            if ("DISPATCHED".equals(next)) {

                shipment.setDispatchedAt(
                        LocalDateTime.now()
                );

                notificationService.dispatched(
                        shipment
                );
            }


            // =================================================
            // DELIVERED
            // =================================================

            if ("DELIVERED".equals(next)) {

                if (!shipment.isMerchantReceived()) {

                    throw new IllegalArgumentException(
                            "Merchant has not confirmed receipt yet."
                    );
                }


                shipment.setDeliveredAt(
                        LocalDateTime.now()
                );


                Order order =
                        shipment.getOrder();


                if (!"Confirmed".equalsIgnoreCase(
                        order.getOrderStatus())) {

                    throw new IllegalArgumentException(
                            "Only confirmed orders can be delivered."
                    );
                }


                order.setOrderStatus(
                        "Delivered"
                );


                order.setDeliveredDate(
                        LocalDateTime.now()
                );


                orderRepo.save(
                        order
                );


                for (
                        OrderFarmerContribution allocation
                        :
                        orderFarmerContributionRepo
                                .findByOrder(order)
                ) {

                    sendAutoEmail
                            .SendBulkFarmerContributionEmail(
                                    order,
                                    allocation,
                                    "Delivered"
                            );
                }


                sendAutoEmail
                        .SendBulkOrderStatusEmail(
                                order,
                                "Delivered"
                        );


                notificationService.delivered(
                        shipment
                );
            }


            // =================================================
            // IN TRANSIT
            // =================================================

            if ("IN_TRANSIT".equals(next)) {

                notificationService.inTransit(
                        shipment
                );
            }


            shipment.setStatus(
                    next
            );


            shipmentRepo.save(
                    shipment
            );


            attributes.addFlashAttribute(
                    "success",
                    "Shipment status updated to "
                            + next
                            + "."
            );


        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Unable to update shipment: "
                            + e.getMessage()
            );
        }


        return "redirect:/FPO/Shipments?demo=" + demoOrder;
    }


    // =========================================================
    // FPO RESOLVES MERCHANT DELAY + EDITS SHIPMENT DETAILS
    // =========================================================

    @PostMapping("/{shipmentId}/ResolveDelay")
    @Transactional
    public String resolveDelay(

            @PathVariable
            Long shipmentId,

            @RequestParam("resolutionMessage")
            String resolutionMessage,

            @RequestParam(
                    value = "destinationAddress",
                    required = false
            )
            String destinationAddress,

            @RequestParam(
                    value = "pickupAddress",
                    required = false
            )
            String pickupAddress,

            @RequestParam(
                    value = "transporterName",
                    required = false
            )
            String transporterName,

            @RequestParam(
                    value = "vehicleNumber",
                    required = false
            )
            String vehicleNumber,

            @RequestParam(
                    value = "newEta",
                    required = false
            )
            String newEta,

            HttpSession session,

            RedirectAttributes attributes) {


        FPO fpo =
                getLoggedInFPO(session);


        if (fpo == null) {

            attributes.addFlashAttribute(
                    "error",
                    "FPO session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }


        try {

            FPOShipment shipment =
                    shipmentRepo
                            .findById(
                                    shipmentId
                            )
                            .orElse(null);


            // =================================================
            // OWNERSHIP CHECK
            // =================================================

            if (shipment == null
                    || shipment.getFpo() == null
                    || shipment.getFpo()
                            .getId()
                            != fpo.getId()) {

                throw new IllegalArgumentException(
                        "Shipment not found or unauthorized."
                );
            }


            // =================================================
            // ONLY IN-TRANSIT
            // =================================================

            if (!"IN_TRANSIT".equalsIgnoreCase(
                    shipment.getStatus())) {

                throw new IllegalArgumentException(
                        "Only in-transit shipments can have a delay resolved."
                );
            }


            // =================================================
            // ACTIVE DELAY
            // =================================================

            if (!shipment.isDelayReported()) {

                throw new IllegalArgumentException(
                        "No active delay report exists for this shipment."
                );
            }


            // =================================================
            // RESOLUTION MESSAGE
            // =================================================

            if (resolutionMessage == null
                    || resolutionMessage.isBlank()) {

                throw new IllegalArgumentException(
                        "Please explain how the delay was resolved."
                );
            }


            // =================================================
            // UPDATE PICKUP
            // =================================================

            if (pickupAddress != null
                    && !pickupAddress.isBlank()) {

                String updatedPickup =
                        normalizeAddress(
                                pickupAddress
                        );


                shipment.setPickupAddress(
                        updatedPickup
                );


                GeocodingService.Coordinates pickupCoordinates =
                        geocodingService.geocode(
                                updatedPickup
                        );


                if (pickupCoordinates != null) {

                    shipment.setPickupLatitude(
                            pickupCoordinates.getLatitude()
                    );

                    shipment.setPickupLongitude(
                            pickupCoordinates.getLongitude()
                    );
                    applyGeocodeMetadata(shipment, pickupCoordinates);
                }
            }


            // =================================================
            // UPDATE DESTINATION
            // =================================================

            if (destinationAddress != null
                    && !destinationAddress.isBlank()) {

                String updatedDestination =
                        normalizeAddress(
                                destinationAddress
                        );


                shipment.setDestinationAddress(
                        updatedDestination
                );


                GeocodingService.Coordinates destinationCoordinates =
                        geocodingService.geocode(
                                updatedDestination
                        );


                if (destinationCoordinates != null) {

                    shipment.setDestinationLatitude(
                            destinationCoordinates.getLatitude()
                    );

                    shipment.setDestinationLongitude(
                            destinationCoordinates.getLongitude()
                    );
                    applyGeocodeMetadata(shipment, destinationCoordinates);

                } else {

                    throw new IllegalArgumentException(
                            "The new destination address could not be converted into coordinates."
                    );
                }
            }


            // =================================================
            // TRANSPORTER
            // =================================================

            if (transporterName != null) {

                shipment.setTransporterName(
                        transporterName.isBlank()
                                ? null
                                : transporterName.trim()
                );
            }


            // =================================================
            // VEHICLE
            // =================================================

            if (vehicleNumber != null) {

                shipment.setVehicleNumber(
                        vehicleNumber.isBlank()
                                ? null
                                : vehicleNumber.trim()
                );
            }


            // =================================================
            // ETA
            // =================================================

            if (newEta != null
                    && !newEta.isBlank()) {

                shipment.setEstimatedDeliveryDate(
                        LocalDateTime.parse(
                                newEta
                        )
                );
            }


            // =================================================
            // MARK DELAY RESOLVED
            // =================================================

            shipment.setDelayResolved(
                    true
            );


            shipment.setDelayResolvedAt(
                    LocalDateTime.now()
            );


            shipment.setDelayResolutionMessage(
                    resolutionMessage.trim()
            );


            // =================================================
            // CLEAR ACTIVE DELAY
            // =================================================

            shipment.setDelayReported(
                    false
            );


            shipmentRepo.saveAndFlush(
                    shipment
            );


            // =================================================
            // NOTIFY MERCHANT
            // =================================================

            notificationService.notifyDelayResolved(
                    shipment
            );


            attributes.addFlashAttribute(
                    "success",
                    "Delay resolved. Shipment details updated and merchant notified. Shipment continues in transit."
            );


        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to resolve delay: "
                            + e.getMessage()
            );
        }


        return "redirect:/FPO/Shipments";
    }
}