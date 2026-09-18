package com.softpro.ATH.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Order;

import jakarta.mail.internet.MimeMessage;

@Service
public class ShipmentNotificationService {

    @Autowired
    private JavaMailSender mailSender;

    private void send(String to, String subject, String html) {

        if (to == null || to.isBlank()) {
            System.err.println(
                    "Shipment email NOT sent: recipient email is empty."
            );
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true);

            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

            System.out.println(
                    "Shipment email sent successfully to: " + to
            );

        } catch (Exception e) {
            System.err.println(
                    "Shipment email failed for " + to + ": " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private String sellerName(FPOShipment shipment) {

        if (shipment.getFpo() != null) {
            return shipment.getFpo().getName();
        }

        if (shipment.getFarmer() != null) {
            return shipment.getFarmer().getName();
        }

        return "Seller";
    }

    private String sellerEmail(FPOShipment shipment) {

        if (shipment.getFpo() != null) {
            String email = shipment.getFpo().getEmail();

            System.out.println(
                    "FPO shipment notification recipient: " + email
            );

            return email;
        }

        if (shipment.getFarmer() != null) {
            String email = shipment.getFarmer().getEmail();

            System.out.println(
                    "Farmer shipment notification recipient: " + email
            );

            return email;
        }

        System.err.println(
                "No FPO/Farmer attached to shipment."
        );

        return null;
    }

    private String merchantName(Order order) {
        return order.getMerchant() != null
                ? order.getMerchant().getName()
                : "Merchant";
    }

    public void dispatched(FPOShipment shipment) {

        Order order = shipment.getOrder();

        send(
                order.getMerchant() == null
                        ? null
                        : order.getMerchant().getEmail(),
                "🚚 AgriTrade Hub: Order #" + order.getOrderId() + " Dispatched",
                "<html><body>"
                        + "<h2>Order Dispatched</h2>"
                        + "<p>Your order has left "
                        + sellerName(shipment)
                        + ".</p>"
                        + "<p><strong>Destination:</strong> "
                        + shipment.getDestinationAddress()
                        + "</p>"
                        + "</body></html>"
        );
    }

    public void inTransit(FPOShipment shipment) {

        Order order = shipment.getOrder();

        send(
                order.getMerchant() == null
                        ? null
                        : order.getMerchant().getEmail(),
                "🛣️ AgriTrade Hub: Order #" + order.getOrderId() + " Is On The Way",
                "<html><body>"
                        + "<h2>Order Is On The Way</h2>"
                        + "<p>Your order is now <strong>IN TRANSIT</strong>.</p>"
                        + "<p>Confirm receipt after arrival or report a delay from My Orders.</p>"
                        + "</body></html>"
        );
    }

    public void merchantReceived(FPOShipment shipment) {

        Order order = shipment.getOrder();

        send(
                sellerEmail(shipment),
                "✅ AgriTrade Hub: Merchant Received Order #" + order.getOrderId(),
                "<html><body>"
                        + "<h2>Goods Received</h2>"
                        + "<p>Merchant "
                        + merchantName(order)
                        + " has confirmed receipt for Order #"
                        + order.getOrderId()
                        + ".</p>"
                        + "<p>You may now mark the shipment Delivered.</p>"
                        + "</body></html>"
        );
    }

    public void delay(FPOShipment shipment) {

        Order order = shipment.getOrder();
        String recipient = sellerEmail(shipment);

        System.out.println(
                "Sending delay notification for Order #"
                        + order.getOrderId()
                        + " to " + recipient
        );

        send(
                recipient,
                "⚠️ AgriTrade Hub: Delivery Delay for Order #"
                        + order.getOrderId(),
                "<html><body>"
                        + "<h2 style='color:#ef6c00;'>⚠️ Delivery Delay Reported</h2>"
                        + "<p>Merchant <strong>"
                        + merchantName(order)
                        + "</strong> has reported a delivery issue.</p>"
                        + "<p><strong>Order:</strong> #"
                        + order.getOrderId()
                        + "</p>"
                        + "<p><strong>Reason:</strong> "
                        + shipment.getDelayReason()
                        + "</p>"
                        + "<p><strong>Merchant Message:</strong> "
                        + shipment.getDelayMessage()
                        + "</p>"
                        + "<p><strong>Current Status:</strong> "
                        + shipment.getStatus()
                        + "</p>"
                        + "<p>Please open your shipment dashboard to review and resolve this issue.</p>"
                        + "</body></html>"
        );
    }

    public void notifyDelayResolved(FPOShipment shipment) {

        Order order = shipment.getOrder();

        String eta =
                shipment.getEstimatedDeliveryDate() == null
                        ? "Not set"
                        : shipment.getEstimatedDeliveryDate().toString();

        String resolution =
                shipment.getDelayResolutionMessage() == null
                        ? "The delivery issue has been resolved and the shipment continues."
                        : shipment.getDelayResolutionMessage();

        send(
                order.getMerchant() == null
                        ? null
                        : order.getMerchant().getEmail(),
                "✅ AgriTrade Hub: Delay Resolved for Order #"
                        + order.getOrderId(),
                "<html><body>"
                        + "<h2 style='color:#2e7d32;'>✅ Delivery Delay Resolved</h2>"
                        + "<p>Your reported delivery issue for Order #"
                        + order.getOrderId()
                        + " has been resolved by <strong>"
                        + sellerName(shipment)
                        + "</strong>.</p>"
                        + "<p><strong>Resolution:</strong> "
                        + resolution
                        + "</p>"
                        + "<p><strong>Updated ETA:</strong> "
                        + eta
                        + "</p>"
                        + "<p>Your shipment continues normally. Please confirm receipt once the goods arrive.</p>"
                        + "</body></html>"
        );
    }

    public void delivered(FPOShipment shipment) {

        Order order = shipment.getOrder();

        send(
                order.getMerchant() == null
                        ? null
                        : order.getMerchant().getEmail(),
                "📦 AgriTrade Hub: Order #" + order.getOrderId() + " Delivered",
                "<html><body>"
                        + "<h2>Order Delivered</h2>"
                        + "<p>Your order has been marked <strong>Delivered</strong> by "
                        + sellerName(shipment)
                        + ".</p>"
                        + "</body></html>"
        );
    }
}