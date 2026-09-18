package com.softpro.ATH.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fpo_shipments")
public class FPOShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(
            name = "order_id",
            nullable = false,
            unique = true
    )
    private Order order;

    @ManyToOne
    @JoinColumn(name = "fpo_id")
    private FPO fpo;

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private Formers farmer;

    @Column(nullable = false, length = 1000)
    private String pickupAddress;

    @Column(nullable = false, length = 1000)
    private String destinationAddress;

    /*
     * =========================================================
     * ROUTE COORDINATES
     * =========================================================
     */

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    /*
     * Geocoding verification metadata. These fields describe the
     * most recently verified coordinate pair on this shipment.
     */
    @Column(name = "geocode_confidence_score")
    private Double geocodeConfidenceScore;

    @Column(name = "geocode_source", length = 50)
    private String geocodeSource;

    @Column(name = "geocoded_at")
    private LocalDateTime geocodedAt;

    /*
     * =========================================================
     * LOGISTICS
     * =========================================================
     */

    @Column(length = 150)
    private String transporterName;

    @Column(length = 100)
    private String vehicleNumber;

    private LocalDateTime estimatedDeliveryDate;

    private LocalDateTime dispatchedAt;

    private LocalDateTime deliveredAt;

    @Column(nullable = false, length = 30)
    private String status = "READY_FOR_PICKUP";

    /*
     * =========================================================
     * MERCHANT RECEIPT
     * =========================================================
     */

    @Column(nullable = false)
    private boolean merchantReceived = false;

    private LocalDateTime merchantReceivedAt;

    /*
     * =========================================================
     * DELAY
     * =========================================================
     */

    @Column(nullable = false)
    private boolean delayReported = false;

    @Column(length = 100)
    private String delayReason;

    @Column(length = 2000)
    private String delayMessage;

    private LocalDateTime delayReportedAt;

    @Column(nullable = false)
    private boolean delayResolved = false;

    private LocalDateTime delayResolvedAt;

    @Column(length = 2000)
    private String delayResolutionMessage;

    /*
     * =========================================================
     * CREATED
     * =========================================================
     */

    @Column(nullable = false)
    private LocalDateTime createdAt =
            LocalDateTime.now();

    /*
     * =========================================================
     * ROUTE OPTIMIZATION
     * =========================================================
     */

    @Column(nullable = false)
    private boolean routeOptimized = false;

    private Integer routeSequence;

    private LocalDateTime routeOptimizedAt;

    /*
     * =========================================================
     * LIVE TRACKING
     * =========================================================
     */

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    public FPOShipment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public FPO getFpo() {
        return fpo;
    }

    public void setFpo(FPO fpo) {
        this.fpo = fpo;
    }

    public Formers getFarmer() {
        return farmer;
    }

    public void setFarmer(Formers farmer) {
        this.farmer = farmer;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(
            Double destinationLatitude) {

        this.destinationLatitude =
                destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(
            Double destinationLongitude) {

        this.destinationLongitude =
                destinationLongitude;
    }

    public Double getGeocodeConfidenceScore() {
        return geocodeConfidenceScore;
    }

    public void setGeocodeConfidenceScore(
            Double geocodeConfidenceScore) {
        this.geocodeConfidenceScore = geocodeConfidenceScore;
    }

    public String getGeocodeSource() {
        return geocodeSource;
    }

    public void setGeocodeSource(String geocodeSource) {
        this.geocodeSource = geocodeSource;
    }

    public LocalDateTime getGeocodedAt() {
        return geocodedAt;
    }

    public void setGeocodedAt(LocalDateTime geocodedAt) {
        this.geocodedAt = geocodedAt;
    }

    public String getTransporterName() {
        return transporterName;
    }

    public void setTransporterName(String transporterName) {
        this.transporterName = transporterName;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public LocalDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(
            LocalDateTime estimatedDeliveryDate) {

        this.estimatedDeliveryDate =
                estimatedDeliveryDate;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(
            LocalDateTime dispatchedAt) {

        this.dispatchedAt =
                dispatchedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(
            LocalDateTime deliveredAt) {

        this.deliveredAt =
                deliveredAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isMerchantReceived() {
        return merchantReceived;
    }

    public void setMerchantReceived(
            boolean merchantReceived) {

        this.merchantReceived =
                merchantReceived;
    }

    public LocalDateTime getMerchantReceivedAt() {
        return merchantReceivedAt;
    }

    public void setMerchantReceivedAt(
            LocalDateTime merchantReceivedAt) {

        this.merchantReceivedAt =
                merchantReceivedAt;
    }

    public boolean isDelayReported() {
        return delayReported;
    }

    public void setDelayReported(
            boolean delayReported) {

        this.delayReported =
                delayReported;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public void setDelayReason(String delayReason) {
        this.delayReason = delayReason;
    }

    public String getDelayMessage() {
        return delayMessage;
    }

    public void setDelayMessage(String delayMessage) {
        this.delayMessage = delayMessage;
    }

    public LocalDateTime getDelayReportedAt() {
        return delayReportedAt;
    }

    public void setDelayReportedAt(
            LocalDateTime delayReportedAt) {

        this.delayReportedAt =
                delayReportedAt;
    }

    public boolean isDelayResolved() {
        return delayResolved;
    }

    public void setDelayResolved(
            boolean delayResolved) {

        this.delayResolved =
                delayResolved;
    }

    public LocalDateTime getDelayResolvedAt() {
        return delayResolvedAt;
    }

    public void setDelayResolvedAt(
            LocalDateTime delayResolvedAt) {

        this.delayResolvedAt =
                delayResolvedAt;
    }

    public String getDelayResolutionMessage() {
        return delayResolutionMessage;
    }

    public void setDelayResolutionMessage(
            String delayResolutionMessage) {

        this.delayResolutionMessage =
                delayResolutionMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt =
                createdAt;
    }

    public boolean isRouteOptimized() {
        return routeOptimized;
    }

    public void setRouteOptimized(
            boolean routeOptimized) {

        this.routeOptimized =
                routeOptimized;
    }

    public Integer getRouteSequence() {
        return routeSequence;
    }

    public void setRouteSequence(
            Integer routeSequence) {

        this.routeSequence =
                routeSequence;
    }

    public LocalDateTime getRouteOptimizedAt() {
        return routeOptimizedAt;
    }

    public void setRouteOptimizedAt(
            LocalDateTime routeOptimizedAt) {

        this.routeOptimizedAt =
                routeOptimizedAt;
    }

    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }
    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }
    public LocalDateTime getLastLocationUpdate() { return lastLocationUpdate; }
    public void setLastLocationUpdate(LocalDateTime lastLocationUpdate) { this.lastLocationUpdate = lastLocationUpdate; }
}