package com.softpro.ATH.Repository;

import com.softpro.ATH.Model.FPOProduct;
import com.softpro.ATH.Model.FPOProductContribution;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FPOProductContributionRepo extends JpaRepository<FPOProductContribution, Long> {

    List<FPOProductContribution> findByFpoProduct(FPOProduct fpoProduct);

    FPOProductContribution findByFpoProductAndFarmer(FPOProduct fpoProduct, Formers farmer);

    long countByFpoProduct(FPOProduct fpoProduct);

    FPOProductContribution findBySourceProduct(Product sourceProduct);

    List<FPOProductContribution> findByFarmer(Formers farmer);

    List<FPOProductContribution> findByFarmer_Id(Long farmerId);

    List<FPOProductContribution> findBySourceProduct_Farmer(Formers farmer);
}