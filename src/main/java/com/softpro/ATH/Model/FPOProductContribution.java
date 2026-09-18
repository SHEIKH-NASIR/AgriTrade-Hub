package com.softpro.ATH.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "fpo_product_contributions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fpo_product_id", "farmer_id"})
})
public class FPOProductContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fpo_product_id", nullable = false)
    private FPOProduct fpoProduct;

    @ManyToOne(optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Formers farmer;

    @ManyToOne(optional = true)
    @JoinColumn(name = "source_product_id")
    private Product sourceProduct;

    @Column(nullable = false)
    private int quantityContributed;

    @Column(nullable = false)
    private int quantityAvailable;

    public FPOProductContribution() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FPOProduct getFpoProduct() {
        return fpoProduct;
    }

    public void setFpoProduct(FPOProduct fpoProduct) {
        this.fpoProduct = fpoProduct;
    }

    public Formers getFarmer() {
        return farmer;
    }

    public void setFarmer(Formers farmer) {
        this.farmer = farmer;
    }

    public Product getSourceProduct() {
        return sourceProduct;
    }

    public void setSourceProduct(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
    }

    public int getQuantityContributed() {
        return quantityContributed;
    }

    public void setQuantityContributed(int quantityContributed) {
        this.quantityContributed = quantityContributed;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }
}