package com.softpro.ATH.Dto;

public class RouteStop {

    private int sequence;

    private Long shipmentId;

    private Long orderId;

    private String merchantName;

    private String destinationAddress;

    private double distanceFromPreviousKm;

    private double estimatedDriveTimeMinutes;

    public RouteStop() {
    }

    public RouteStop(
            int sequence,
            Long shipmentId,
            Long orderId,
            String merchantName,
            String destinationAddress,
            double distanceFromPreviousKm,
            double estimatedDriveTimeMinutes) {

        this.sequence = sequence;
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.merchantName = merchantName;
        this.destinationAddress = destinationAddress;
        this.distanceFromPreviousKm = distanceFromPreviousKm;
        this.estimatedDriveTimeMinutes = estimatedDriveTimeMinutes;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public double getDistanceFromPreviousKm() {
        return distanceFromPreviousKm;
    }

    public void setDistanceFromPreviousKm(
            double distanceFromPreviousKm) {

        this.distanceFromPreviousKm = distanceFromPreviousKm;
    }

    public double getEstimatedDriveTimeMinutes() {
        return estimatedDriveTimeMinutes;
    }

    public void setEstimatedDriveTimeMinutes(
            double estimatedDriveTimeMinutes) {
        this.estimatedDriveTimeMinutes = estimatedDriveTimeMinutes;
    }
}