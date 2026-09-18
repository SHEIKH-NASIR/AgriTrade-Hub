package com.softpro.ATH.Dto;

import java.util.ArrayList;
import java.util.List;

public class RouteOptimizationResponse {

    private List<RouteStop> stops = new ArrayList<>();

    private double totalDistanceKm;

    private double totalEstimatedTimeMinutes;

    private String googleMapsUrl;

    private String message;

    public RouteOptimizationResponse() {
    }

    public RouteOptimizationResponse(
            List<RouteStop> stops,
            double totalDistanceKm,
            String googleMapsUrl,
            String message) {

        this.stops = stops;
        this.totalDistanceKm = totalDistanceKm;
        this.googleMapsUrl = googleMapsUrl;
        this.message = message;
    }

    public List<RouteStop> getStops() {
        return stops;
    }

    public void setStops(List<RouteStop> stops) {
        this.stops = stops;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(
            double totalDistanceKm) {

        this.totalDistanceKm = totalDistanceKm;
    }

    public double getTotalEstimatedTimeMinutes() {
        return totalEstimatedTimeMinutes;
    }

    public void setTotalEstimatedTimeMinutes(
            double totalEstimatedTimeMinutes) {
        this.totalEstimatedTimeMinutes = totalEstimatedTimeMinutes;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public void setGoogleMapsUrl(
            String googleMapsUrl) {

        this.googleMapsUrl = googleMapsUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}