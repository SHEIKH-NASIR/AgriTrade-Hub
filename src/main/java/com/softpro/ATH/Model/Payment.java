package com.softpro.ATH.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String paymentMode; // e.g., UPI, Bank Transfer, COD

    private String payId;

    @Column(length = 30)
    private String paymentSource = "RAZORPAY";
    
    private long transactionId;

    private BigDecimal amount;

    @Column(precision = 14, scale = 2)
    private BigDecimal productAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal logisticsAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private LocalDateTime paymentDate = LocalDateTime.now();

    public Payment() {
    }

	public Long getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(Long paymentId) {
		this.paymentId = paymentId;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public String getPaymentMode() {
		return paymentMode;
	}

	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}

	public long getTransactionId() {
		return transactionId;
	}
	
	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

	public String getPayId() {
		return payId;
	}

	public void setPayId(String payId) {
		this.payId = payId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDateTime getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(LocalDateTime paymentDate) {
		this.paymentDate = paymentDate;
	}

	public BigDecimal getProductAmount() { return productAmount; }
	public void setProductAmount(BigDecimal productAmount) { this.productAmount = productAmount; }
	public BigDecimal getLogisticsAmount() { return logisticsAmount; }
	public void setLogisticsAmount(BigDecimal logisticsAmount) { this.logisticsAmount = logisticsAmount; }
	public BigDecimal getTotalAmount() { return totalAmount; }
	public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    // Getters and Setters
	public String getPaymentSource() {
		return paymentSource;
	}

	public void setPaymentSource(String paymentSource) {
		this.paymentSource = paymentSource;
	}

}
