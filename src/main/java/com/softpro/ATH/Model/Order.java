package com.softpro.ATH.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long orderId;

    @ManyToOne
    @JoinColumn(name = "farmer_id", referencedColumnName = "id")
    private Formers farmer;

    @ManyToOne
    @JoinColumn(name = "merchant_id", referencedColumnName = "id")
    private Merchant merchant;

    @ManyToOne
    @JoinColumn(name = "fpo_id", referencedColumnName = "id")
    private FPO fpo;

    @ManyToOne
    @JoinColumn(name = "fpo_product_id", referencedColumnName = "id")
    private FPOProduct fpoProduct;

    @Column(nullable = false)
    private String productName;

    private int quantity;

    private BigDecimal pricePerUnit;

    @Column(precision = 14, scale = 2)
    private BigDecimal productAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal logisticsAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private Double roadDistanceKm;
    private String vehicleType;

    @Column(nullable = false)
    private String orderStatus;  // Pending, Confirmed, Delivered

    private LocalDateTime orderDate = LocalDateTime.now();
    private LocalDateTime deliveredDate;

    @Column(nullable = false)
    private boolean demoOrder = false;

    public Order() {
    }

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public Formers getFarmer() {
		return farmer;
	}

	public void setFarmer(Formers farmer) {
		this.farmer = farmer;
	}

	public Merchant getMerchant() {
		return merchant;
	}

	public void setMerchant(Merchant merchant) {
		this.merchant = merchant;
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

	public String getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}

	public LocalDateTime getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}

	public LocalDateTime getDeliveredDate() {
		return deliveredDate;
	}

	public void setDeliveredDate(LocalDateTime deliveredDate) {
		this.deliveredDate = deliveredDate;
	}

	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}
	public BigDecimal getProductAmount() { return productAmount; }
	public void setProductAmount(BigDecimal productAmount) { this.productAmount = productAmount; }

	public BigDecimal getLogisticsAmount() { return logisticsAmount; }
	public void setLogisticsAmount(BigDecimal logisticsAmount) { this.logisticsAmount = logisticsAmount; }

	public BigDecimal getTotalAmount() { return totalAmount; }
	public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

	public Double getRoadDistanceKm() { return roadDistanceKm; }
	public void setRoadDistanceKm(Double roadDistanceKm) { this.roadDistanceKm = roadDistanceKm; }

	public String getVehicleType() { return vehicleType; }
	public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    // Getters and Setters
	public boolean isDemoOrder() {
		return demoOrder;
	}

	public void setDemoOrder(boolean demoOrder) {
		this.demoOrder = demoOrder;
	}

}
