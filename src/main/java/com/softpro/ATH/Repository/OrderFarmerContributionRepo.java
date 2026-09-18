package com.softpro.ATH.Repository;

import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.OrderFarmerContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderFarmerContributionRepo extends JpaRepository<OrderFarmerContribution, Long> {

    List<OrderFarmerContribution> findByOrder(Order order);

    List<OrderFarmerContribution> findByFarmer(Formers farmer);
}
