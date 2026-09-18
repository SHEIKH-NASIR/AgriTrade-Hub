package com.softpro.ATH.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softpro.ATH.Model.MarketPriceReference;

@Repository
public interface MarketPriceReferenceRepo
        extends JpaRepository<MarketPriceReference, Long> {

    Optional<MarketPriceReference>
    findTopByProductNameIgnoreCaseOrderByUpdatedAtDesc(
            String productName);

    Optional<MarketPriceReference>
    findTopByProductNameIgnoreCaseAndCategoryIgnoreCaseOrderByUpdatedAtDesc(
            String productName,
            String category);
}