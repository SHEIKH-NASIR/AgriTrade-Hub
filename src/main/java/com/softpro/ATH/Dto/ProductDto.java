package com.softpro.ATH.Dto;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import com.softpro.ATH.Model.Formers;

public class ProductDto {

    private Long id;

    private String productName;

    private int quantity;

    private BigDecimal pricePerUnit;

    private String category;

    private Formers farmer;

    private String status;

    private MultipartFile image;

    // =========================================================
    // QUANTITY / MARKETPLACE RULES
    // =========================================================

    /*
     * Minimum quantity a normal marketplace buyer can order.
     */
    private Integer minimumOrderQuantity = 1;

    /*
     * Maximum quantity a normal marketplace buyer can order.
     *
     * 0 or null means no separately configured maximum.
     */
    private Integer maxOrderQuantity = 0;

    /*
     * Minimum quantity required for a bulk order.
     */
    private Integer bulkMinimumQuantity = 1;

    /*
     * Maximum quantity allowed for a bulk order.
     *
     * 0 or null means no separately configured maximum.
     */
    private Integer maxBulkQuantity = 0;

    /*
     * Price per unit applicable to bulk orders.
     */
    private BigDecimal bulkPricePerUnit;

    /*
     * INDEPENDENT or FPO.
     */
    private String sellingMode = "INDEPENDENT";

    /*
     * Selected FPO when selling mode is FPO.
     */
    private Long fpoId;

    // =========================================================
    // ID
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // =========================================================
    // PRODUCT NAME
    // =========================================================

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    // =========================================================
    // QUANTITY
    // =========================================================

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // =========================================================
    // NORMAL PRICE
    // =========================================================

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    // =========================================================
    // CATEGORY
    // =========================================================

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // =========================================================
    // FARMER
    // =========================================================

    public Formers getFarmer() {
        return farmer;
    }

    public void setFarmer(Formers farmer) {
        this.farmer = farmer;
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // =========================================================
    // IMAGE
    // =========================================================

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    // =========================================================
    // MINIMUM ORDER QUANTITY
    // =========================================================

    public Integer getMinimumOrderQuantity() {
        return minimumOrderQuantity;
    }

    public void setMinimumOrderQuantity(Integer minimumOrderQuantity) {
        this.minimumOrderQuantity = minimumOrderQuantity;
    }

    // =========================================================
    // MAXIMUM ORDER QUANTITY
    // =========================================================

    public Integer getMaxOrderQuantity() {
        return maxOrderQuantity;
    }

    public void setMaxOrderQuantity(Integer maxOrderQuantity) {
        this.maxOrderQuantity = maxOrderQuantity;
    }

    // =========================================================
    // BULK MINIMUM QUANTITY
    // =========================================================

    public Integer getBulkMinimumQuantity() {
        return bulkMinimumQuantity;
    }

    public void setBulkMinimumQuantity(Integer bulkMinimumQuantity) {
        this.bulkMinimumQuantity = bulkMinimumQuantity;
    }

    // =========================================================
    // BULK MAXIMUM QUANTITY
    // =========================================================

    public Integer getMaxBulkQuantity() {
        return maxBulkQuantity;
    }

    public void setMaxBulkQuantity(Integer maxBulkQuantity) {
        this.maxBulkQuantity = maxBulkQuantity;
    }

    // =========================================================
    // BULK PRICE PER UNIT
    // =========================================================

    public BigDecimal getBulkPricePerUnit() {
        return bulkPricePerUnit;
    }

    public void setBulkPricePerUnit(BigDecimal bulkPricePerUnit) {
        this.bulkPricePerUnit = bulkPricePerUnit;
    }

    // =========================================================
    // SELLING MODE
    // =========================================================

    public String getSellingMode() {
        return sellingMode;
    }

    public void setSellingMode(String sellingMode) {
        this.sellingMode = sellingMode;
    }

    // =========================================================
    // FPO
    // =========================================================

    public Long getFpoId() {
        return fpoId;
    }

    public void setFpoId(Long fpoId) {
        this.fpoId = fpoId;
    }
}