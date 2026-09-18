package com.softpro.ATH.Repository;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.FPOProduct;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FPOProductRepo extends JpaRepository<FPOProduct, Long> {

    List<FPOProduct> findByFpo(FPO fpo);

    List<FPOProduct> findByStatus(String status);

    List<FPOProduct> findByFpoAndStatus(FPO fpo, String status);
}