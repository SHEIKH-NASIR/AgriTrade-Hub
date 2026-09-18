package com.softpro.ATH.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;

public interface FPOShipmentRepo
        extends JpaRepository<FPOShipment, Long> {

    Optional<FPOShipment> findByOrder(Order order);

    List<FPOShipment> findByFpoOrderByCreatedAtDesc(
            FPO fpo
    );

    List<FPOShipment> findByFarmerOrderByCreatedAtDesc(
            Formers farmer
    );

    Optional<FPOShipment> findByIdAndOrderMerchant(
            Long id,
            Merchant merchant
    );

    @Query("""
        SELECT s
        FROM FPOShipment s
        WHERE s.fpo = :fpo
          AND s.status = :status
          AND s.order.demoOrder = :demo
        ORDER BY s.createdAt ASC
    """)
    List<FPOShipment> findRouteShipmentsForFpo(
            @Param("fpo") FPO fpo,
            @Param("status") String status,
            @Param("demo") boolean demo
    );

    @Query("""
        SELECT s
        FROM FPOShipment s
        WHERE s.farmer = :farmer
          AND s.status = :status
          AND s.order.demoOrder = :demo
        ORDER BY s.createdAt ASC
    """)
    List<FPOShipment> findRouteShipmentsForFarmer(
            @Param("farmer") Formers farmer,
            @Param("status") String status,
            @Param("demo") boolean demo
    );
}