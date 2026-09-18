package com.softpro.ATH.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "market_price_references")
public class MarketPriceReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String productName;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal referencePrice;

    @Column(nullable = false)
    private double lowerPercentage = 10.0;

    @Column(nullable = false)
    private double upperPercentage = 10.0;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public MarketPriceReference() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getReferencePrice() {
        return referencePrice;
    }

    public void setReferencePrice(BigDecimal referencePrice) {
        this.referencePrice = referencePrice;
    }

    public double getLowerPercentage() {
        return lowerPercentage;
    }

    public void setLowerPercentage(double lowerPercentage) {
        this.lowerPercentage = lowerPercentage;
    }

    public double getUpperPercentage() {
        return upperPercentage;
    }

    public void setUpperPercentage(double upperPercentage) {
        this.upperPercentage = upperPercentage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}