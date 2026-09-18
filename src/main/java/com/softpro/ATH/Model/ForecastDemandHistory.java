package com.softpro.ATH.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "forecast_demand_history")
public class ForecastDemandHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Exactly one of these should normally be populated.
     *
     * farmer = historical demand belonging to an
     * independent farmer.
     *
     * fpo = historical demand belonging to an FPO.
     */
    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private Formers farmer;

    @ManyToOne
    @JoinColumn(name = "fpo_id")
    private FPO fpo;

    @Column(nullable = false, length = 150)
    private String productName;

    /*
     * Date on which this historical demand occurred.
     */
    @Column(nullable = false)
    private LocalDate demandDate;

    /*
     * Quantity demanded on that date.
     */
    @Column(nullable = false)
    private int quantity;

    /*
     * Optional historical price.
     * It is useful as an AI feature but is not mandatory.
     */
    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal pricePerUnit;

    /*
     * ADMIN / IMPORT / HISTORICAL
     */
    @Column(nullable = false, length = 30)
    private String source = "HISTORICAL";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ForecastDemandHistory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Formers getFarmer() {
        return farmer;
    }

    public void setFarmer(Formers farmer) {
        this.farmer = farmer;
    }

    public FPO getFpo() {
        return fpo;
    }

    public void setFpo(FPO fpo) {
        this.fpo = fpo;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public LocalDate getDemandDate() {
        return demandDate;
    }

    public void setDemandDate(LocalDate demandDate) {
        this.demandDate = demandDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public java.math.BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(java.math.BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}