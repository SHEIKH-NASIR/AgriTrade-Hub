package com.softpro.ATH.Model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "seller_reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"order_id", "merchant_id"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private Formers farmer;

    @ManyToOne
    @JoinColumn(name = "fpo_id")
    private FPO fpo;

    @Column(nullable = false, length = 20)
    private String sellerType;

    @Column(nullable = false)
    private int rating;

    private Integer qualityRating;
    private Integer quantityAccuracyRating;
    private Integer timelinessRating;

    @Column(length = 1500)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Merchant getMerchant() { return merchant; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }
    public Formers getFarmer() { return farmer; }
    public void setFarmer(Formers farmer) { this.farmer = farmer; }
    public FPO getFpo() { return fpo; }
    public void setFpo(FPO fpo) { this.fpo = fpo; }
    public String getSellerType() { return sellerType; }
    public void setSellerType(String sellerType) { this.sellerType = sellerType; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public Integer getQualityRating() { return qualityRating; }
    public void setQualityRating(Integer qualityRating) { this.qualityRating = qualityRating; }
    public Integer getQuantityAccuracyRating() { return quantityAccuracyRating; }
    public void setQuantityAccuracyRating(Integer quantityAccuracyRating) { this.quantityAccuracyRating = quantityAccuracyRating; }
    public Integer getTimelinessRating() { return timelinessRating; }
    public void setTimelinessRating(Integer timelinessRating) { this.timelinessRating = timelinessRating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
