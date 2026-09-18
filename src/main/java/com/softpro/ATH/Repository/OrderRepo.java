package com.softpro.ATH.Repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;

public interface OrderRepo extends JpaRepository<Order, Long>{

	List<Order> findByMerchant(Merchant merchant);

	List<Order> findByMerchantAndDemoOrder(Merchant merchant, boolean demoOrder);

	long countByFpoAndDemoOrder(FPO fpo, boolean demoOrder);

	long countByFpoAndDemoOrderAndOrderStatus(FPO fpo, boolean demoOrder, String orderStatus);

	long countByFarmerAndDemoOrder(Formers farmer, boolean demoOrder);

	long countByFarmerAndDemoOrderAndOrderStatus(Formers farmer, boolean demoOrder, String orderStatus);

	List<Order> findTop5ByFarmerAndDemoOrderOrderByOrderDateDesc(Formers farmer, boolean demoOrder);

	long countByOrderStatusAndDemoOrder(String orderStatus, boolean demoOrder);

	List<Order> findByFarmer(Formers farmer);

	List<Order> findByFarmerAndDemoOrder(Formers farmer, boolean demoOrder);

	List<Order> findByFpo(FPO fpo);

	List<Order> findByFpoAndDemoOrder(FPO fpo, boolean demoOrder);

	long countByFpo(FPO fpo);

	long countByFpoAndOrderStatus(FPO fpo, String orderStatus);

	List<Order> findTop4ByFpoAndOrderStatusOrderByOrderDateDesc(FPO fpo, String orderStatus);

	Order findByOrderId(Long orderId);
	
	@Query("SELECT o FROM Order o WHERE o.farmer = :farmer ORDER BY CASE WHEN o.orderStatus = 'Cancelled' THEN 1 ELSE 0 END")
	List<Order> findByFarmerOrdered(@Param("farmer") Formers farmer);

	Object countByOrderStatus(String string);

	List<Order> findTop5ByFarmerOrderByOrderDateDesc(Formers farmer);

	Object countByFarmer(Formers farmer);

	Object countByFarmerAndOrderStatus(Formers farmer, String string);
	
	List<Order> findTop4ByFarmerAndOrderStatusOrderByOrderDateDesc(Formers farmer, String orderStatus);

	
	@Query("SELECT SUM(o.pricePerUnit * o.quantity) FROM Order o WHERE o.farmer.id = :id AND o.demoOrder = false AND o.orderStatus = 'Delivered'")
	BigDecimal getTotalRevenueByFarmerId(@Param("id") Long farmerId);

	@Query("SELECT SUM(o.pricePerUnit * o.quantity) FROM Order o WHERE o.farmer.id = :id AND o.demoOrder = false AND o.orderStatus = 'Delivered' AND MONTH(o.deliveredDate) = MONTH(CURRENT_DATE) AND YEAR(o.deliveredDate) = YEAR(CURRENT_DATE)")
	BigDecimal getCurrentMonthRevenue(@Param("id") Long farmerId);

	@Query("SELECT o.productName FROM Order o WHERE o.farmer.id = :id AND o.demoOrder = false AND o.orderStatus = 'Delivered' GROUP BY o.productName ORDER BY SUM(o.quantity) DESC LIMIT 1")
	String getMostOrderedProduct(@Param("id") Long farmerId);

}
