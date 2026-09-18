package com.softpro.ATH.Controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Repository.FPOShipmentRepo;
import com.softpro.ATH.Service.ShipmentNotificationService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Merchant/Shipments")
public class MerchantShipmentController {

    @Autowired
    private FPOShipmentRepo shipmentRepo;

    @Autowired
    private ShipmentNotificationService notificationService;

    private Merchant merchant(HttpSession session) {

        Object value =
                session.getAttribute(
                        "loggedInMerchant"
                );

        return value instanceof Merchant
                ? (Merchant) value
                : null;
    }


    @PostMapping("/{id}/ConfirmReceived")
    @Transactional
    public String confirm(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes attributes) {

        Merchant merchant =
                merchant(session);

        if (merchant == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Merchant session expired."
            );

            return "redirect:/Mlogin";
        }

        try {

            FPOShipment shipment =
                    shipmentRepo
                            .findByIdAndOrderMerchant(
                                    id,
                                    merchant
                            )
                            .orElse(null);

            if (shipment == null) {
                throw new IllegalArgumentException(
                        "Shipment not found."
                );
            }

            if (!"IN_TRANSIT".equalsIgnoreCase(
                    shipment.getStatus())) {

                throw new IllegalArgumentException(
                        "Receipt can be confirmed only while shipment is IN_TRANSIT."
                );
            }

            if (shipment.isMerchantReceived()) {

                throw new IllegalArgumentException(
                        "Goods receipt has already been confirmed."
                );
            }

            // A reported delay must be resolved by the farmer/FPO
            // before the merchant confirms goods received.
            if (shipment.isDelayReported() && !shipment.isDelayResolved()) {

                throw new IllegalArgumentException(
                        "Please wait for the farmer/FPO to resolve the reported delay before confirming goods receipt."
                );
            }

            shipment.setMerchantReceived(
                    true
            );

            shipment.setMerchantReceivedAt(
                    LocalDateTime.now()
            );

            shipmentRepo.save(
                    shipment
            );

            notificationService.merchantReceived(
                    shipment
            );

            attributes.addFlashAttribute(
                    "success",
                    "Goods receipt confirmed. Farmer/FPO has been notified."
            );

        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to confirm receipt: "
                            + e.getMessage()
            );
        }

        return "redirect:/Merchant/MyOrders";
    }


    @PostMapping("/{id}/Delay")
    @Transactional
    public String delay(
            @PathVariable Long id,
            @RequestParam String delayReason,
            @RequestParam String delayMessage,
            HttpSession session,
            RedirectAttributes attributes) {

        Merchant merchant =
                merchant(session);

        if (merchant == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Merchant session expired."
            );

            return "redirect:/Mlogin";
        }

        try {

            FPOShipment shipment =
                    shipmentRepo
                            .findByIdAndOrderMerchant(
                                    id,
                                    merchant
                            )
                            .orElse(null);

            if (shipment == null) {

                throw new IllegalArgumentException(
                        "Shipment not found."
                );
            }

            if (!"IN_TRANSIT".equalsIgnoreCase(
                    shipment.getStatus())) {

                throw new IllegalArgumentException(
                        "Delay can be reported only while shipment is IN_TRANSIT."
                );
            }

            if (shipment.isMerchantReceived()) {

                throw new IllegalArgumentException(
                        "Goods receipt has already been confirmed."
                );
            }

            if (delayReason == null
                    || delayReason.isBlank()) {

                throw new IllegalArgumentException(
                        "Delay reason is required."
                );
            }

            if (delayMessage == null
                    || delayMessage.isBlank()) {

                throw new IllegalArgumentException(
                        "Delay message is required."
                );
            }

            shipment.setDelayReported(
                    true
            );

            shipment.setDelayResolved(
                    false
            );

            shipment.setDelayReason(
                    delayReason.trim()
            );

            shipment.setDelayMessage(
                    delayMessage.trim()
            );

            shipment.setDelayReportedAt(
                    LocalDateTime.now()
            );

            shipment.setDelayResolvedAt(
                    null
            );

            shipment.setDelayResolutionMessage(
                    null
            );

            shipmentRepo.save(
                    shipment
            );

            notificationService.delay(
                    shipment
            );

            attributes.addFlashAttribute(
                    "success",
                    "Delay report submitted successfully. Farmer/FPO has been notified."
            );

        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to report delay: "
                            + e.getMessage()
            );
        }

        return "redirect:/Merchant/MyOrders";
    }
}