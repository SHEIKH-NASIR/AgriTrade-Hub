package com.softpro.ATH.Dto;

import java.math.BigDecimal;

import com.softpro.ATH.Model.VehicleType;

public class LogisticsCostResponse {

    private boolean success;
    private String message;

    private double roadDistanceKm;
    private double estimatedDurationMinutes;

    private VehicleType vehicleType;

    private int quantityKg;

    private BigDecimal baseCost;
    private BigDecimal distanceCost;
    private BigDecimal logisticsCost;

    public LogisticsCostResponse() {
    }

    public static LogisticsCostResponse success(
            double roadDistanceKm,
            double estimatedDurationMinutes,
            VehicleType vehicleType,
            int quantityKg,
            BigDecimal baseCost,
            BigDecimal distanceCost,
            BigDecimal logisticsCost) {

        LogisticsCostResponse response =
                new LogisticsCostResponse();

        response.success = true;
        response.message = "Logistics cost calculated successfully.";

        response.roadDistanceKm = roadDistanceKm;
        response.estimatedDurationMinutes =
                estimatedDurationMinutes;

        response.vehicleType = vehicleType;
        response.quantityKg = quantityKg;

        response.baseCost = baseCost;
        response.distanceCost = distanceCost;
        response.logisticsCost = logisticsCost;

        return response;
    }

    public static LogisticsCostResponse failure(
            String message) {

        LogisticsCostResponse response =
                new LogisticsCostResponse();

        response.success = false;
        response.message = message;

        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getRoadDistanceKm() {
        return roadDistanceKm;
    }

    public void setRoadDistanceKm(double roadDistanceKm) {
        this.roadDistanceKm = roadDistanceKm;
    }

    public double getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(
            double estimatedDurationMinutes) {

        this.estimatedDurationMinutes =
                estimatedDurationMinutes;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public int getQuantityKg() {
        return quantityKg;
    }

    public void setQuantityKg(int quantityKg) {
        this.quantityKg = quantityKg;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public BigDecimal getDistanceCost() {
        return distanceCost;
    }

    public void setDistanceCost(BigDecimal distanceCost) {
        this.distanceCost = distanceCost;
    }

    public BigDecimal getLogisticsCost() {
        return logisticsCost;
    }

    public void setLogisticsCost(BigDecimal logisticsCost) {
        this.logisticsCost = logisticsCost;
    }
}