package com.softpro.ATH.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Repository.FPOShipmentRepo;
import com.softpro.ATH.Repository.OrderRepo;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class ShipmentViewAdvice {

    @Autowired private FPOShipmentRepo shipmentRepo;
    @Autowired private OrderRepo orderRepo;

    @ModelAttribute
    public void addShipmentMap(
            HttpSession session,
            Map<String, Object> model) {

        Object merchantObj = session.getAttribute("loggedInMerchant");
        if (merchantObj instanceof Merchant merchant) {
            Map<Long, FPOShipment> shipmentMap = new HashMap<>();
            for (Order order : orderRepo.findByMerchant(merchant)) {
                shipmentRepo.findByOrder(order).ifPresent(
                        shipment -> shipmentMap.put(order.getOrderId(), shipment)
                );
            }
            model.put("shipmentMap", shipmentMap);
            return;
        }

        Object farmerObj = session.getAttribute("loggedInFormer");
        if (!(farmerObj instanceof Formers)) {
            farmerObj = session.getAttribute("former");
        }
        if (farmerObj instanceof Formers farmer) {
            Map<Long, FPOShipment> shipmentMap = new HashMap<>();
            for (Order order : orderRepo.findByFarmer(farmer)) {
                shipmentRepo.findByOrder(order).ifPresent(
                        shipment -> shipmentMap.put(order.getOrderId(), shipment)
                );
            }
            model.put("shipmentMap", shipmentMap);
        }
    }
}