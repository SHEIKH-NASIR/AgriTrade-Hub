package com.softpro.ATH.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_farmer_contributions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"order_id", "farmer_id"})
})
public class OrderFarmerContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Formers farmer;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    public OrderFarmerContribution() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Formers getFarmer() {
        return farmer;
    }

    public void setFarmer(Formers farmer) {
        this.farmer = farmer;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
