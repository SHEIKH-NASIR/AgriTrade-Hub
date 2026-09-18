package com.softpro.ATH.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Review;

public interface ReviewRepo extends JpaRepository<Review, Long> {

    Optional<Review> findByOrderAndMerchant(Order order, Merchant merchant);

    List<Review> findByFarmerOrderByCreatedAtDesc(Formers farmer);

    List<Review> findByFpoOrderByCreatedAtDesc(FPO fpo);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.farmer.id = :sellerId")
    Double averageForFarmer(@Param("sellerId") Long sellerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.fpo.id = :sellerId")
    Double averageForFpo(@Param("sellerId") Long sellerId);

    long countByFarmer(Formers farmer);
    long countByFpo(FPO fpo);
}
