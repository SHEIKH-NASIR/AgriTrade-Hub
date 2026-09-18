package com.softpro.ATH.Controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Dto.RouteOptimizationResponse;
import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Repository.FPOShipmentRepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Service.DemoAccessService;
import com.softpro.ATH.Service.GeocodingService;
import com.softpro.ATH.Service.RouteOptimizationService;
import com.softpro.ATH.Service.ShipmentNotificationService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Former/Shipments")
public class FormerShipmentController {

    @Autowired
    private FPOShipmentRepo shipmentRepo;

    @Autowired
    private FormersRepo formersRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ShipmentNotificationService notificationService;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private RouteOptimizationService routeOptimizationService;

    @Autowired
    private DemoAccessService demoAccessService;


    // =========================================================
    // LOGGED-IN FARMER
    // =========================================================

    private Formers farmer(HttpSession session) {

        Object loggedInFormer =
                session.getAttribute("loggedInFormer");

        if (loggedInFormer instanceof Formers) {
            return (Formers) loggedInFormer;
        }

        Object former =
                session.getAttribute("former");

        if (former instanceof Formers) {
            return (Formers) former;
        }

        return null;
    }


    // =========================================================
    // ALL SHIPMENTS
    // =========================================================

    @GetMapping
    @Transactional
    public String list(
            @RequestParam(name = "demo", defaultValue = "false") boolean demo,
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        Formers farmer = farmer(session);

        if (farmer == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Farmer session expired."
            );

            return "redirect:/Flogin";
        }

        if (demo) {
            demoAccessService.requireFarmer(farmer);
        }

        List<FPOShipment> shipments =
                shipmentRepo.findByFarmerOrderByCreatedAtDesc(
                        farmer
                );

        shipments = shipments.stream()
                .filter(s -> s != null
                        && s.getOrder() != null
                        && s.getOrder().isDemoOrder() == demo)
                .toList();

        // ---------------------------------------------------------
        // MIGRATE OLD INDEPENDENT-FARMER SHIPMENTS
        // ---------------------------------------------------------
        // Older farmer shipments were saved as "Created".  The
        // shipment lifecycle uses READY_FOR_PICKUP as the first
        // actionable state, so convert only shipments belonging to
        // this farmer.  FPO shipments are not touched because this
        // query is restricted to the logged-in farmer.
        boolean migratedOldStatus = false;

        for (FPOShipment shipment : shipments) {
            if (shipment.getStatus() == null
                    || "CREATED".equalsIgnoreCase(
                            shipment.getStatus().trim())) {

                shipment.setStatus("READY_FOR_PICKUP");
                shipmentRepo.save(shipment);
                migratedOldStatus = true;
            }
        }

        if (migratedOldStatus) {
            shipmentRepo.flush();
        }

        model.addAttribute(
                "shipments",
                shipments
        );

        model.addAttribute("demoMode", demo);

        model.addAttribute(
                "former",
                farmer
        );

        return "Former/shipments";
    }


    // =========================================================
    // CREATE SHIPMENT PAGE
    //
    // Works for:
    // 1. Real independent farmer orders
    // 2. Demo orders
    //
    // Demo order never changes Product inventory.
    // =========================================================

    @GetMapping("/Create")
    @Transactional(readOnly = true)
    public String createPage(
            @RequestParam Long orderId,
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        Formers farmer = farmer(session);

        if (farmer == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Farmer session expired."
            );

            return "redirect:/Flogin";
        }

        Order order =
                orderRepo.findByOrderId(orderId);

        if (order == null
                || order.getFarmer() == null
                || order.getFarmer().getId()
                        != farmer.getId()) {

            attributes.addFlashAttribute(
                    "error",
                    "Order not found or unauthorized."
            );

            return "redirect:/Former/Orders";
        }

        if (!"Confirmed".equalsIgnoreCase(
                order.getOrderStatus())) {

            attributes.addFlashAttribute(
                    "error",
                    "Only confirmed orders can be shipped."
            );

            return "redirect:/Former/Orders";
        }

        if (shipmentRepo.findByOrder(order).isPresent()) {

            attributes.addFlashAttribute(
                    "error",
                    "Shipment already exists."
            );

            return "redirect:/Former/Shipments";
        }

        model.addAttribute(
                "order",
                order
        );

        model.addAttribute(
                "former",
                farmer
        );

        model.addAttribute(
                "merchantAddress",
                order.getMerchant() != null
                        ? order.getMerchant().getAddress()
                        : ""
        );

        model.addAttribute(
                "merchantName",
                order.getMerchant() != null
                        ? order.getMerchant().getName()
                        : "Merchant"
        );

        /*
         * Important:
         *
         * isDemoOrder() is read only.
         * It does NOT touch farmer inventory.
         */
        model.addAttribute(
                "demoOrder",
                order.isDemoOrder()
        );

        return "Former/create-shipment";
    }


    // =========================================================
    // CREATE SHIPMENT
    // =========================================================

    @PostMapping("/Create")
    @Transactional
    public String create(
            @RequestParam Long orderId,
            @RequestParam String pickupAddress,
            @RequestParam(required = false)
                    String destinationAddress,
            @RequestParam(required = false)
                    String transporterName,
            @RequestParam(required = false)
                    String vehicleNumber,
            @RequestParam(required = false)
                    String estimatedDeliveryDate,
            HttpSession session,
            RedirectAttributes attributes) {

        Formers farmer = farmer(session);

        if (farmer == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Farmer session expired."
            );

            return "redirect:/Flogin";
        }

        try {

            Order order =
                    orderRepo.findByOrderId(orderId);

            if (order == null
                    || order.getFarmer() == null
                    || order.getFarmer().getId()
                            != farmer.getId()) {

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
                        "Shipment already exists."
                );
            }

            if (pickupAddress == null
                    || pickupAddress.isBlank()) {

                throw new IllegalArgumentException(
                        "Pickup address is required."
                );
            }


            // =================================================
            // MERCHANT DESTINATION
            // =================================================

            String merchantAddress = null;

            if (order.getMerchant() != null
                    && order.getMerchant().getAddress() != null
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

            shipment.setOrder(order);
            shipment.setFarmer(farmer);

            /*
             * Independent farmer shipments do not belong to an FPO.
             */
            shipment.setFpo(null);

            shipment.setPickupAddress(
                    finalPickupAddress
            );

            shipment.setDestinationAddress(
                    normalizedDestination
            );


            // =================================================
            // DEMO FLAG
            // =================================================

            boolean demoOrder =
                    order.isDemoOrder();

            /*
             * VERY IMPORTANT:
             *
             * Demo orders are shipment/route demonstrations only.
             *
             * We DO NOT modify:
             *
             * Product.quantity
             * Product.availableQuantity
             * Product.status
             *
             * here.
             *
             * Real order inventory behaviour remains untouched.
             */


            System.out.println(
                    "================================================="
            );

            System.out.println(
                    "CREATING FARMER SHIPMENT"
            );

            System.out.println(
                    "Order ID: "
                            + order.getOrderId()
            );

            System.out.println(
                    "Demo Order: "
                            + demoOrder
            );

            System.out.println(
                    "Pickup: "
                            + finalPickupAddress
            );

            System.out.println(
                    "Destination: "
                            + normalizedDestination
            );

            System.out.println(
                    "================================================="
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

                shipment.setGeocodeConfidenceScore(
                        pickupCoordinates
                                .getConfidenceScore()
                );

                shipment.setGeocodeSource(
                        pickupCoordinates.getSource()
                );

                shipment.setGeocodedAt(
                        LocalDateTime.now()
                );
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

                shipment.setGeocodeConfidenceScore(
                        destinationCoordinates
                                .getConfidenceScore()
                );

                shipment.setGeocodeSource(
                        destinationCoordinates.getSource()
                );

                shipment.setGeocodedAt(
                        LocalDateTime.now()
                );

            } else {

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
                            ? "Demo Transporter"
                            : transporterName.trim()
            );

            shipment.setVehicleNumber(
                    vehicleNumber == null
                            || vehicleNumber.isBlank()
                            ? "DEMO-VEHICLE"
                            : vehicleNumber.trim()
            );


            // =================================================
            // STATUS
            // =================================================
            // A newly created farmer shipment must be actionable
            // immediately.  "Created" was the old legacy value and
            // prevented the Dispatch button from appearing.
            shipment.setStatus(
                    "READY_FOR_PICKUP"
            );

            shipment.setRouteOptimized(
                    false
            );

            shipment.setCreatedAt(
                    LocalDateTime.now()
            );


            // =================================================
            // ESTIMATED DELIVERY
            // =================================================

            if (estimatedDeliveryDate != null
                    && !estimatedDeliveryDate.isBlank()) {

                try {

                    shipment.setEstimatedDeliveryDate(
                             java.time.LocalDate.parse(
                                      estimatedDeliveryDate
                            ).atStartOfDay()

                    );

                } catch (Exception ignored) {

                    System.out.println(
                            "Invalid estimated delivery date: "
                                    + estimatedDeliveryDate
                    );
                }
            }


            shipmentRepo.save(shipment);


            attributes.addFlashAttribute(
                    "msg",
                    demoOrder
                            ? "Demo shipment created successfully. Farmer inventory was not changed."
                            : "Shipment created successfully."
            );

            return "redirect:/Former/Shipments?demo=" + demoOrder;

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Unable to create shipment: "
                            + e.getMessage()
            );

            return "redirect:/Former/Orders";
        }
    }


    // =========================================================
    // RE-VERIFY LOCATIONS
    //
    // FIX:
    // /Former/Shipments/ReverifyLocations
    //
    // Re-geocodes existing shipments for selected mode.
    // Does NOT change:
    // - order status
    // - shipment status
    // - inventory
    // - route order
    // - Google Maps flow
    // - delivery flow
    // =========================================================

    @PostMapping("/ReverifyLocations")
    @Transactional
    public String reverifyLocations(
            @RequestParam(
                    value = "demo",
                    defaultValue = "false")
                    boolean demo,
            HttpSession session,
            RedirectAttributes attributes) {

        Formers farmer = farmer(session);

        if (farmer == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Farmer session expired."
            );

            return "redirect:/Flogin";
        }

        try {

            if (demo) {
                demoAccessService.requireFarmer(farmer);
            }

            List<FPOShipment> shipments =
                    shipmentRepo.findByFarmerOrderByCreatedAtDesc(
                            farmer
                    );

            int verified = 0;
            int failed = 0;

            for (FPOShipment shipment : shipments) {

                if (shipment == null
                        || shipment.getOrder() == null) {
                    continue;
                }

                /*
                 * Only re-verify shipments belonging to the
                 * currently selected Demo / Real mode.
                 */
                if (shipment.getOrder().isDemoOrder() != demo) {
                    continue;
                }

                boolean updated = false;


                // =================================================
                // PICKUP LOCATION
                // =================================================

                if (shipment.getPickupAddress() != null
                        && !shipment.getPickupAddress().isBlank()) {

                    GeocodingService.Coordinates pickup =
                            geocodingService.geocode(
                                    normalizeAddress(
                                            shipment.getPickupAddress()
                                    )
                            );

                    if (pickup != null) {

                        shipment.setPickupLatitude(
                                pickup.getLatitude()
                        );

                        shipment.setPickupLongitude(
                                pickup.getLongitude()
                        );

                        updated = true;
                    }
                }


                // =================================================
                // DESTINATION LOCATION
                // =================================================

                if (shipment.getDestinationAddress() != null
                        && !shipment.getDestinationAddress().isBlank()) {

                    GeocodingService.Coordinates destination =
                            geocodingService.geocode(
                                    normalizeAddress(
                                            shipment.getDestinationAddress()
                                    )
                            );

                    if (destination != null) {

                        shipment.setDestinationLatitude(
                                destination.getLatitude()
                        );

                        shipment.setDestinationLongitude(
                                destination.getLongitude()
                        );

                        updated = true;
                    }
                }


                // =================================================
                // SAVE ONLY LOCATION DATA
                // =================================================

                if (updated) {

                    shipment.setGeocodedAt(
                            LocalDateTime.now()
                    );

                    shipmentRepo.save(shipment);

                    verified++;

                } else {

                    failed++;
                }
            }


            attributes.addFlashAttribute(
                    "success",
                    verified
                            + " location(s) re-verified successfully."
                            + (failed > 0
                                    ? " "
                                      + failed
                                      + " location(s) could not be verified."
                                    : "")
            );

            return "redirect:/Former/Shipments?demo=" + demo;

        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to re-verify locations: "
                            + e.getMessage()
            );

            return "redirect:/Former/Shipments?demo=" + demo;
        }
    }


    // =========================================================
    // OPTIMIZE ROUTE
    //
    // Example:
    // /Former/Shipments/OptimizeRoute?demo=true
    //
    // OR
    //
    // /Former/Shipments/OptimizeRoute?demo=false
    //
    // =========================================================

    @GetMapping("/OptimizeRoute")
    @Transactional
    public String optimizeRoute(
            @RequestParam(
                    value = "demo",
                    defaultValue = "false")
                    boolean demo,
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        Formers farmer = farmer(session);

        if (farmer == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Farmer session expired."
            );

            return "redirect:/Flogin";
        }

        try {

            List<FPOShipment> shipments =
                    shipmentRepo
                            .findByFarmerOrderByCreatedAtDesc(
                                    farmer
                            );

            /*
             * Only route shipments matching the requested mode.
             */
            List<FPOShipment> routeShipments =
                    new java.util.ArrayList<>();

            for (FPOShipment shipment : shipments) {

                if (shipment == null
                        || shipment.getOrder() == null) {

                    continue;
                }

                boolean shipmentIsDemo =
                        shipment.getOrder()
                                .isDemoOrder();

                if (shipmentIsDemo == demo) {

                    /*
                     * Only active/uncompleted shipments should
                     * participate in route optimization.
                     */
                    String status =
                            shipment.getStatus();

                    if (status == null
                            || !status.equalsIgnoreCase(
                                    "Delivered")) {

                        routeShipments.add(
                                shipment
                        );
                    }
                }
            }


            if (routeShipments.isEmpty()) {

                attributes.addFlashAttribute(
                        "error",
                        demo
                                ? "No demo shipments available for route optimization."
                                : "No real shipments available for route optimization."
                );

                return "redirect:/Former/Shipments";
            }


            // =================================================
            // CHECK COORDINATES
            // =================================================

            for (FPOShipment shipment :
                    routeShipments) {

                if (shipment.getPickupLatitude() == null
                        || shipment.getPickupLongitude() == null
                        || shipment.getDestinationLatitude() == null
                        || shipment.getDestinationLongitude() == null) {

                    /*
                     * Try to geocode again before failing.
                     */
                    if (shipment.getPickupAddress() != null) {

                        GeocodingService.Coordinates pickup =
                                geocodingService.geocode(
                                        shipment.getPickupAddress()
                                );

                        if (pickup != null) {

                            shipment.setPickupLatitude(
                                    pickup.getLatitude()
                            );

                            shipment.setPickupLongitude(
                                    pickup.getLongitude()
                            );
                        }
                    }


                    if (shipment.getDestinationAddress() != null) {

                        GeocodingService.Coordinates destination =
                                geocodingService.geocode(
                                        shipment.getDestinationAddress()
                                );

                        if (destination != null) {

                            shipment.setDestinationLatitude(
                                    destination.getLatitude()
                            );

                            shipment.setDestinationLongitude(
                                    destination.getLongitude()
                            );
                        }
                    }

                    shipmentRepo.save(
                            shipment
                    );
                }
            }


            // =================================================
            // PICKUP
            //
            // All shipments are expected to use the farmer's
            // pickup location.
            // =================================================

            FPOShipment first =
                    routeShipments.get(0);

            Double pickupLatitude =
                    first.getPickupLatitude();

            Double pickupLongitude =
                    first.getPickupLongitude();


            if (pickupLatitude == null
                    || pickupLongitude == null) {

                throw new IllegalArgumentException(
                        "Pickup coordinates are unavailable."
                );
            }


            // =================================================
            // ORS ROUTE OPTIMIZATION
            // =================================================

            System.out.println(
                    "================================================="
            );

            System.out.println(
                    "INDEPENDENT FARMER "
                            + (demo
                                    ? "DEMO "
                                    : "REAL ")
                            + "ROUTE OPTIMIZATION"
            );

            System.out.println(
                    "Pickup coordinates: "
                            + pickupLatitude
                            + ", "
                            + pickupLongitude
            );

            System.out.println(
                    "Destinations: "
                            + routeShipments.size()
            );

            System.out.println(
                    "================================================="
            );


            /*
             * IMPORTANT:
             *
             * RouteOptimizationService is reused.
             *
             * It calculates ROAD distance/time through ORS.
             *
             * It does NOT use straight-line distance.
             */
            RouteOptimizationResponse route =
                    routeOptimizationService.optimizeRoute(
                            routeShipments
                    );

            if (route == null
                    || route.getStops() == null
                    || route.getStops().isEmpty()) {

                attributes.addFlashAttribute(
                        "error",
                        route != null && route.getMessage() != null
                                ? route.getMessage()
                                : "Unable to optimize route."
                );

                return "redirect:/Former/Shipments?demo=" + demo;
            }

            model.addAttribute("route", route);
            model.addAttribute("former", farmer);
            model.addAttribute("demoMode", demo);
            model.addAttribute("active4", "active");

            return "Former/optimized-route";

        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Route optimization failed: "
                            + e.getMessage()
            );

            return "redirect:/Former/Shipments?demo=" + demo;
        }
    }


    // =========================================================
    // UPDATE SHIPMENT STATUS
    //
    // Independent farmer shipment lifecycle mirrors the existing
    // FPO flow:
    // READY_FOR_PICKUP -> DISPATCHED -> IN_TRANSIT -> DELIVERED
    //
    // Delivery is allowed only after merchant confirms receipt.
    // =========================================================

    @PostMapping("/{shipmentId}/Status")
    @Transactional
    public String updateShipmentStatus(
            @PathVariable Long shipmentId,
            @RequestParam("newStatus") String newStatus,
            HttpSession session,
            RedirectAttributes attributes) {

        Formers farmer = farmer(session);
        boolean demoOrder = false;

        if (farmer == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Farmer session expired."
            );
            return "redirect:/Flogin";
        }

        try {
            FPOShipment shipment =
                    shipmentRepo.findById(shipmentId).orElse(null);

            if (shipment == null
                    || shipment.getFarmer() == null
                    || shipment.getFarmer().getId() != farmer.getId()) {

                throw new IllegalArgumentException(
                        "Shipment not found or unauthorized."
                );
            }

            demoOrder =
                    shipment.getOrder() != null
                            && shipment.getOrder().isDemoOrder();

            if (demoOrder) {
                demoAccessService.requireFarmer(farmer);
            }

            String current =
                    shipment.getStatus() == null
                            ? ""
                            : shipment.getStatus().trim().toUpperCase();

            String next =
                    newStatus == null
                            ? ""
                            : newStatus.trim().toUpperCase();

            boolean validTransition =
                    ("READY_FOR_PICKUP".equals(current)
                            && "DISPATCHED".equals(next))
                    ||
                    ("DISPATCHED".equals(current)
                            && "IN_TRANSIT".equals(next))
                    ||
                    ("IN_TRANSIT".equals(current)
                            && "DELIVERED".equals(next))
                    ||
                    (!"DELIVERED".equals(current)
                            && "CANCELLED".equals(next));

            if (!validTransition) {
                throw new IllegalArgumentException(
                        "Invalid shipment status transition."
                );
            }

            // -----------------------------------------------------
            // DISPATCHED
            // -----------------------------------------------------
            if ("DISPATCHED".equals(next)) {
                shipment.setDispatchedAt(LocalDateTime.now());
                notificationService.dispatched(shipment);
            }

            // -----------------------------------------------------
            // DELIVERED
            // Merchant must confirm goods first.
            // -----------------------------------------------------
            if ("DELIVERED".equals(next)) {

                if (!shipment.isMerchantReceived()) {
                    throw new IllegalArgumentException(
                            "Merchant has not confirmed receipt yet."
                    );
                }

                Order order = shipment.getOrder();

                if (order == null) {
                    throw new IllegalArgumentException(
                            "Shipment order is missing."
                    );
                }

                if (!"Confirmed".equalsIgnoreCase(
                        order.getOrderStatus())) {

                    throw new IllegalArgumentException(
                            "Only confirmed orders can be delivered."
                    );
                }

                shipment.setDeliveredAt(LocalDateTime.now());

                order.setOrderStatus("Delivered");
                order.setDeliveredDate(LocalDateTime.now());

                orderRepo.save(order);

                notificationService.delivered(shipment);
            }

            // -----------------------------------------------------
            // IN TRANSIT
            // -----------------------------------------------------
            if ("IN_TRANSIT".equals(next)) {
                notificationService.inTransit(shipment);
            }

            shipment.setStatus(next);
            shipmentRepo.saveAndFlush(shipment);

            attributes.addFlashAttribute(
                    "success",
                    "Shipment status updated to " + next + "."
            );

        } catch (Exception e) {
            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to update shipment: " + e.getMessage()
            );
        }

        return "redirect:/Former/Shipments?demo=" + demoOrder;
    }


    // =========================================================
    // FARMER RESOLVES MERCHANT DELAY + UPDATES SHIPMENT
    //
    // Merchant reports the delay while shipment is IN_TRANSIT.
    // Farmer sees the reason/message and resolves it here.
    // =========================================================

    @PostMapping("/{shipmentId}/ResolveDelay")
    @Transactional
    public String resolveDelay(
            @PathVariable Long shipmentId,
            @RequestParam("resolutionMessage") String resolutionMessage,
            @RequestParam(value = "destinationAddress", required = false)
                    String destinationAddress,
            @RequestParam(value = "pickupAddress", required = false)
                    String pickupAddress,
            @RequestParam(value = "transporterName", required = false)
                    String transporterName,
            @RequestParam(value = "vehicleNumber", required = false)
                    String vehicleNumber,
            @RequestParam(value = "newEta", required = false)
                    String newEta,
            HttpSession session,
            RedirectAttributes attributes) {

        Formers farmer = farmer(session);
        boolean demoOrder = false;

        if (farmer == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Farmer session expired."
            );
            return "redirect:/Flogin";
        }

        try {
            FPOShipment shipment =
                    shipmentRepo.findById(shipmentId).orElse(null);

            if (shipment == null
                    || shipment.getFarmer() == null
                    || shipment.getFarmer().getId() != farmer.getId()) {

                throw new IllegalArgumentException(
                        "Shipment not found or unauthorized."
                );
            }

            demoOrder =
                    shipment.getOrder() != null
                            && shipment.getOrder().isDemoOrder();

            if (demoOrder) {
                demoAccessService.requireFarmer(farmer);
            }

            if (!"IN_TRANSIT".equalsIgnoreCase(
                    shipment.getStatus())) {

                throw new IllegalArgumentException(
                        "Only in-transit shipments can have a delay resolved."
                );
            }

            if (!shipment.isDelayReported()) {
                throw new IllegalArgumentException(
                        "No active delay report exists for this shipment."
                );
            }

            if (resolutionMessage == null
                    || resolutionMessage.isBlank()) {

                throw new IllegalArgumentException(
                        "Please explain how the delay was resolved."
                );
            }

            // -----------------------------------------------------
            // Optional corrected pickup address
            // -----------------------------------------------------
            if (pickupAddress != null
                    && !pickupAddress.isBlank()) {

                String updatedPickup =
                        normalizeAddress(pickupAddress);

                shipment.setPickupAddress(updatedPickup);

                GeocodingService.Coordinates pickupCoordinates =
                        geocodingService.geocode(updatedPickup);

                if (pickupCoordinates != null) {
                    shipment.setPickupLatitude(
                            pickupCoordinates.getLatitude()
                    );
                    shipment.setPickupLongitude(
                            pickupCoordinates.getLongitude()
                    );
                    applyGeocodeMetadata(
                            shipment,
                            pickupCoordinates
                    );
                }
            }

            // -----------------------------------------------------
            // Optional corrected destination
            // -----------------------------------------------------
            if (destinationAddress != null
                    && !destinationAddress.isBlank()) {

                String updatedDestination =
                        normalizeAddress(destinationAddress);

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
                    applyGeocodeMetadata(
                            shipment,
                            destinationCoordinates
                    );
                } else {
                    throw new IllegalArgumentException(
                            "The new destination address could not be converted into coordinates."
                    );
                }
            }

            if (transporterName != null) {
                shipment.setTransporterName(
                        transporterName.isBlank()
                                ? null
                                : transporterName.trim()
                );
            }

            if (vehicleNumber != null) {
                shipment.setVehicleNumber(
                        vehicleNumber.isBlank()
                                ? null
                                : vehicleNumber.trim()
                );
            }

            if (newEta != null && !newEta.isBlank()) {
                shipment.setEstimatedDeliveryDate(
                        LocalDateTime.parse(newEta)
                );
            }

            shipment.setDelayResolved(true);
            shipment.setDelayResolvedAt(LocalDateTime.now());
            shipment.setDelayResolutionMessage(
                    resolutionMessage.trim()
            );

            // Clear active delay so the shipment can proceed.
            shipment.setDelayReported(false);

            shipmentRepo.saveAndFlush(shipment);

            notificationService.notifyDelayResolved(
                    shipment
            );

            attributes.addFlashAttribute(
                    "success",
                    "Delay resolved. Shipment details updated, merchant notified, and shipment continues in transit."
            );

        } catch (Exception e) {
            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to resolve delay: " + e.getMessage()
            );
        }

        return "redirect:/Former/Shipments?demo=" + demoOrder;
    }


    // =========================================================
    // DELIVERY
    // =========================================================

    private void applyGeocodeMetadata(
            FPOShipment shipment,
            GeocodingService.Coordinates coordinates) {

        shipment.setGeocodeConfidenceScore(
                coordinates.getConfidenceScore()
        );
        shipment.setGeocodeSource(
                coordinates.getSource()
        );
        shipment.setGeocodedAt(
                LocalDateTime.now()
        );
    }


    // =========================================================
    // ADDRESS NORMALIZATION
    // =========================================================

    private String normalizeAddress(
            String address) {

        if (address == null) {
            return null;
        }

        String normalized =
                address
                        .replace('\n', ' ')
                        .replace('\r', ' ')
                        .replace('\t', ' ')
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        if (normalized.isBlank()) {
            return null;
        }

        /*
         * Always give geocoders Indian context where the user
         * has entered a short Lucknow address.
         */
        String lower =
                normalized.toLowerCase();

        if (lower.contains("lucknow")
                && !lower.contains(
                        "uttar pradesh")) {

            normalized +=
                    ", Uttar Pradesh";
        }

        if (lower.contains("lucknow")
                && !lower.contains(
                        "india")) {

            normalized +=
                    ", India";
        }

        return normalized;
    }
}