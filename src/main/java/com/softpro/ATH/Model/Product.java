package com.softpro.ATH.Model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal pricePerUnit;

    @Column(nullable = false)
    private String category;

    @ManyToOne
    @JoinColumn(name = "farmer_id", referencedColumnName = "id")
    private Formers farmer;

    @Column(nullable = false)
    private String status;

    @Column(length = 1000)
    private String image;

    /*
     * Minimum quantity allowed for a normal merchant order.
     */
    @Column(nullable = false)
    private int minimumOrderQuantity = 1;

    /*
     * Maximum quantity allowed for a normal merchant order.
     *
     * A value <= 0 means no separately configured maximum.
     * The available stock is still always enforced.
     */
    @Column(nullable = false)
    private int maxOrderQuantity = 0;

    /*
     * Minimum quantity required when this product is used
     * for a bulk/FPO order.
     */
    @Column(nullable = false)
    private int bulkMinimumQuantity = 1;

    /*
     * Maximum quantity allowed for a bulk order from this
     * farmer's product.
     *
     * A value <= 0 means no separately configured maximum.
     */
    @Column(nullable = false)
    private int maxBulkQuantity = 0;

    /*
     * Price per unit applicable to bulk orders.
     * If not separately configured, this can be the same
     * as pricePerUnit.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal bulkPricePerUnit;

    /*
     * INDEPENDENT or FPO
     */
    @Column(length = 20)
    private String sellingMode = "INDEPENDENT";

    @ManyToOne
    @JoinColumn(name = "fpo_id")
    private FPO fpo;

    @ManyToOne
    @JoinColumn(name = "fpo_product_id")
    private FPOProduct fpoProduct;

    public Product() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Formers getFarmer() {
        return farmer;
    }

    public void setFarmer(Formers farmer) {
        this.farmer = farmer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getMinimumOrderQuantity() {
        return minimumOrderQuantity;
    }

    public void setMinimumOrderQuantity(int minimumOrderQuantity) {
        this.minimumOrderQuantity = minimumOrderQuantity;
    }

    public int getMaxOrderQuantity() {
        return maxOrderQuantity;
    }

    public void setMaxOrderQuantity(int maxOrderQuantity) {
        this.maxOrderQuantity = maxOrderQuantity;
    }

    public int getBulkMinimumQuantity() {
        return bulkMinimumQuantity;
    }

    public void setBulkMinimumQuantity(int bulkMinimumQuantity) {
        this.bulkMinimumQuantity = bulkMinimumQuantity;
    }

    public int getMaxBulkQuantity() {
        return maxBulkQuantity;
    }

    public void setMaxBulkQuantity(int maxBulkQuantity) {
        this.maxBulkQuantity = maxBulkQuantity;
    }

    public BigDecimal getBulkPricePerUnit() {
        return bulkPricePerUnit;
    }

    public void setBulkPricePerUnit(BigDecimal bulkPricePerUnit) {
        this.bulkPricePerUnit = bulkPricePerUnit;
    }

    public String getSellingMode() {
        return sellingMode;
    }

    public void setSellingMode(String sellingMode) {
        this.sellingMode = sellingMode;
    }

    public FPO getFpo() {
        return fpo;
    }

    public void setFpo(FPO fpo) {
        this.fpo = fpo;
    }

    public FPOProduct getFpoProduct() {
        return fpoProduct;
    }

    public void setFpoProduct(FPOProduct fpoProduct) {
        this.fpoProduct = fpoProduct;
    }
}