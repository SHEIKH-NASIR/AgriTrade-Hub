package com.softpro.ATH.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;

public class OrderDto {
	
	private Long orderId;

    private Formers farmer;

    private Merchant merchant;

    private String productName;

    private int quantity;

    private BigDecimal pricePerUnit;

    private String orderStatus;  // Pending, Confirmed, Delivered

    private LocalDateTime orderDate;

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

}
