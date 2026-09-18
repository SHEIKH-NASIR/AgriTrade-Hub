package com.softpro.ATH.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Payment;

public interface PaymentRepo extends JpaRepository<Payment, Long>{

	Payment findByOrder(Order order);

	
}
