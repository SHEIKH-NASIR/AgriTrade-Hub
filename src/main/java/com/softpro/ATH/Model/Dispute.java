package com.softpro.ATH.Model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "order_disputes")
public class Dispute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @ManyToOne(optional = false) @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;
    @ManyToOne @JoinColumn(name = "farmer_id")
    private Formers farmer;
    @ManyToOne @JoinColumn(name = "fpo_id")
    private FPO fpo;

    @Column(nullable = false, length = 80) private String reason;
    @Column(nullable = false, length = 2500) private String description;
    @Column(length = 255) private String evidenceReference;
    @Column(nullable = false, length = 30) private String status = "OPEN";
    @Column(length = 2500) private String adminResolution;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Order getOrder(){return order;} public void setOrder(Order order){this.order=order;}
    public Merchant getMerchant(){return merchant;} public void setMerchant(Merchant merchant){this.merchant=merchant;}
    public Formers getFarmer(){return farmer;} public void setFarmer(Formers farmer){this.farmer=farmer;}
    public FPO getFpo(){return fpo;} public void setFpo(FPO fpo){this.fpo=fpo;}
    public String getReason(){return reason;} public void setReason(String reason){this.reason=reason;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public String getEvidenceReference(){return evidenceReference;} public void setEvidenceReference(String evidenceReference){this.evidenceReference=evidenceReference;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getAdminResolution(){return adminResolution;} public void setAdminResolution(String adminResolution){this.adminResolution=adminResolution;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public LocalDateTime getResolvedAt(){return resolvedAt;} public void setResolvedAt(LocalDateTime resolvedAt){this.resolvedAt=resolvedAt;}
}
