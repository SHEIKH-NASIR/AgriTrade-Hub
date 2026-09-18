package com.softpro.ATH.Controller;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Model.Dispute;
import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Repository.DisputeRepo;
import com.softpro.ATH.Repository.FPOShipmentRepo;
import com.softpro.ATH.Repository.OrderRepo;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Merchant/Disputes")
public class DisputeController {
    @Autowired private OrderRepo orderRepo;
    @Autowired private DisputeRepo disputeRepo;
    @Autowired private FPOShipmentRepo shipmentRepo;

    @PostMapping("/{orderId}")
    @Transactional
    public String create(@PathVariable Long orderId,
                         @RequestParam String reason,
                         @RequestParam String description,
                         @RequestParam(required=false) Double quantityReceived,
                         @RequestParam(required=false) String evidenceReference,
                         HttpSession session, RedirectAttributes attributes) {
        Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
        if (merchant == null) return "redirect:/Mlogin";
        try {
            Order order = orderRepo.findByOrderId(orderId);
            if (order == null || order.getMerchant() == null || order.getMerchant().getId() != merchant.getId())
                throw new IllegalArgumentException("Order not found.");
            if (!("Delivered".equalsIgnoreCase(order.getOrderStatus()) || order.getDeliveredDate() != null))
                throw new IllegalArgumentException("A dispute can be raised only after delivery.");
            if (reason == null || reason.isBlank() || description == null || description.isBlank())
                throw new IllegalArgumentException("Reason and description are required.");
            if (disputeRepo.findByOrder(order).isPresent())
                throw new IllegalArgumentException("A dispute already exists for this order.");

            Dispute d = new Dispute();
            d.setOrder(order); d.setMerchant(merchant);
            d.setFarmer(order.getFarmer()); d.setFpo(order.getFpo());
            String finalDescription = description.trim();
            if (quantityReceived != null) {
                finalDescription += "\\n\\nQuantity ordered: " + order.getQuantity()
                        + "\\nQuantity received: " + quantityReceived;
            }
            d.setReason(reason.trim()); d.setDescription(finalDescription);
            d.setEvidenceReference(evidenceReference == null ? null : evidenceReference.trim());
            d.setCreatedAt(LocalDateTime.now());
            disputeRepo.save(d);
            attributes.addFlashAttribute("success", "Dispute submitted to Admin Enquiry successfully.");
        } catch (Exception e) { attributes.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/Merchant/MyOrders";
    }
}
