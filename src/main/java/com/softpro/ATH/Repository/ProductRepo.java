package com.softpro.ATH.Repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Product;

public interface ProductRepo extends JpaRepository<Product, Long> {

    // Existing farmer product lookup
    List<Product> findByFarmer(Formers farmer);

    // Paginated farmer product lookup
    Page<Product> findByFarmer(Formers farmer, Pageable pageable);

    // Farmer products by status
    List<Product> findByFarmerAndStatus(
            Formers farmer,
            String status);

    // Products by status
    List<Product> findByStatus(String status);

    // Specific farmer product
    Product findByProductNameAndFarmer(
            String productName,
            Formers farmer);

    // Count farmer products
    long countByFarmer(Formers farmer);

    // Category + status
    List<Product> findByCategoryAndStatus(
            String categoryName,
            String status);


    @Query("SELECT p FROM Product p WHERE p.status = :status AND (p.sellingMode IS NULL OR p.sellingMode = :sellingMode)")
    List<Product> findMarketplaceProducts(@Param("status") String status, @Param("sellingMode") String sellingMode);

    @Query("SELECT p FROM Product p WHERE p.fpo = :fpo AND p.sellingMode = :sellingMode AND p.status = 'Available' AND p.fpoProduct IS NULL ORDER BY p.productName, p.farmer.name")
    List<Product> findCommittedProductsForFpo(@Param("fpo") FPO fpo, @Param("sellingMode") String sellingMode);

    /*
     * FPO committed produce should be determined by FPO membership,
     * not only by Product.fpo_id.
     *
     * This allows an existing farmer product with sellingMode=FPO
     * to appear in the correct FPO even when the product was saved
     * before fpo_id was populated.
     */
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.farmer.id IN :memberIds
          AND p.sellingMode = :sellingMode
          AND p.status = 'Available'
          AND p.quantity > 0
          AND p.fpoProduct IS NULL
        ORDER BY p.productName, p.farmer.name
        """)
    List<Product> findCommittedProductsForFpoMembers(
            @Param("memberIds") List<Long> memberIds,
            @Param("sellingMode") String sellingMode);

    List<Product> findByFpoAndSellingModeAndStatus(FPO fpo, String sellingMode, String status);

    List<Product> findByFarmerAndSellingMode(Formers farmer, String sellingMode);

    // In-stock revenue
    @Query("""
        SELECT SUM(p.pricePerUnit * p.quantity)
        FROM Product p
        WHERE p.farmer.id = :farmerId
        AND p.status = 'Available'
        """)
    BigDecimal calculateInStockRevenue(
            @Param("farmerId") Long farmerId);
}