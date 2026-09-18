package com.softpro.ATH.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fpo_products")
public class FPOProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fpo_id", nullable = false)
    private FPO fpo;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private BigDecimal pricePerUnit;

    /*
     * Minimum quantity required for a merchant bulk order.
     */
    @Column(nullable = false)
    private int minimumBulkQuantity;

    /*
     * Maximum quantity that can be purchased in one
     * merchant bulk order.
     *
     * A value <= 0 means no separately configured maximum.
     * Available stock is still always enforced.
     */
    @Column(nullable = false)
    private int maxBulkQuantity = 0;

    @Column(nullable = false)
    private String status = "Available";

    private LocalDateTime createdAt = LocalDateTime.now();

    public FPOProduct() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public int getMinimumBulkQuantity() {
        return minimumBulkQuantity;
    }

    public void setMinimumBulkQuantity(int minimumBulkQuantity) {
        this.minimumBulkQuantity = minimumBulkQuantity;
    }

    public int getMaxBulkQuantity() {
        return maxBulkQuantity;
    }

    public void setMaxBulkQuantity(int maxBulkQuantity) {
        this.maxBulkQuantity = maxBulkQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}