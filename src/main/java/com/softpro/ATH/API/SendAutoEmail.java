package com.softpro.ATH.API;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.OrderFarmerContribution;
import com.softpro.ATH.Model.Payment;
import com.softpro.ATH.Model.Product;

import jakarta.mail.internet.MimeMessage;

@Service
public class SendAutoEmail {
	
	@Autowired
	private JavaMailSender mailSender;
	
	public void SendOrderConfirmationEmail(Order order) 
	{
		// Assuming these types:
		int quantity = order.getQuantity();
		BigDecimal pricePerUnit = order.getPricePerUnit();
		// Convert quantity to BigDecimal
		BigDecimal quantityBD = new BigDecimal(quantity);
		// Multiply quantity * pricePerUnit
		BigDecimal totalPrice = pricePerUnit.multiply(quantityBD);
		
		String subject = "🌿 AgriTrade Hub: Your Order Has Been Confirmed & Is Being Processed";
		String message = "<html>" +
		    "<body style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; background-color: #e8f5e9; margin: 0; padding: 20px;'>" +
		    "  <div style='max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,128,0,0.2); padding: 30px;'>" +
		    "    <h1 style='color: #2e7d32; border-bottom: 3px solid #81c784; padding-bottom: 12px; margin-bottom: 30px; font-weight: 700;'>Order Confirmation</h1>" +
		    "    <p style='font-size: 16px; color: #424242;'>Hello <strong>" + order.getMerchant().getName() + "</strong>,</p>" +
		    "    <p style='font-size: 16px; color: #424242;'>Thank you for choosing <span style='color:#388e3c; font-weight: 600;'>AgriTrade Hub</span>. Your order has been <span style='color:#2e7d32; font-weight: 700;'>confirmed</span> and is now being processed.</p>" +
		    "    <h2 style='color: #1b5e20; margin-top: 35px;'>Order Details</h2>" +
		    "    <table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>" +
		    "      <thead>" +
		    "        <tr style='background-color: #a5d6a7;'>" +
		    "          <th style='padding: 14px; border: 1px solid #81c784; text-align: left; color: #1b5e20;'>Product Name</th>" +
		    "          <th style='padding: 14px; border: 1px solid #81c784; text-align: left; color: #1b5e20;'>Quantity</th>" +
		    "          <th style='padding: 14px; border: 1px solid #81c784; text-align: left; color: #1b5e20;'>Price (₹)</th>" +
		    "        </tr>" +
		    "      </thead>" +
		    "      <tbody>" +
		    "        <tr>" +
		    "          <td style='padding: 14px; border: 1px solid #c8e6c9;'>" + order.getProductName() + "</td>" +
		    "          <td style='padding: 14px; border: 1px solid #c8e6c9;'>" + order.getQuantity() + "</td>" +
		    "          <td style='padding: 14px; border: 1px solid #c8e6c9;'>₹" + order.getPricePerUnit() + "</td>" +
		    "        </tr>" +
		    "        <tr style='background-color: #e8f5e9; font-weight: 700;'>" +
		    "          <td colspan='2' style='padding: 14px; border: 1px solid #81c784; text-align: right; color: #2e7d32;'>Total Amount</td>" +
		    "          <td style='padding: 14px; border: 1px solid #81c784; color: #2e7d32;'>₹" + totalPrice + "</td>" +
		    "        </tr>" +
		    "      </tbody>" +
		    "    </table>" +

		    "    <h2 style='color: #1b5e20; margin-top: 40px;'>Farmer Details</h2>" +
		    "    <table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>" +
		    "      <tbody>" +
		    "        <tr style='background-color: #a5d6a7;'>" +
		    "          <th style='padding: 12px; border: 1px solid #81c784; text-align: left; color: #1b5e20;'>Farmer Name</th>" +
		    "          <td style='padding: 12px; border: 1px solid #c8e6c9;'>" + order.getFarmer().getName() + "</td>" +
		    "        </tr>" +
		    "        <tr style='background-color: #e8f5e9;'>" +
		    "          <th style='padding: 12px; border: 1px solid #81c784; text-align: left; color: #1b5e20;'>Contact</th>" +
		    "          <td style='padding: 12px; border: 1px solid #c8e6c9;'>" + order.getFarmer().getContactno() + "</td>" +
		    "        </tr>" +
		    "        <tr style='background-color: #a5d6a7;'>" +
		    "          <th style='padding: 12px; border: 1px solid #81c784; text-align: left; color: #1b5e20;'>Email</th>" +
		    "          <td style='padding: 12px; border: 1px solid #c8e6c9;'>" + order.getFarmer().getEmail() + "</td>" +
		    "        </tr>" +
		    "        <tr style='background-color: #e8f5e9;'>" +
		    "          <th style='padding: 12px; border: 1px solid #81c784; text-align: left; color: #1b5e20;'>Location</th>" +
		    "          <td style='padding: 12px; border: 1px solid #c8e6c9;'>" + order.getFarmer().getAddress() + "</td>" +
		    "        </tr>" +
		    "      </tbody>" +
		    "    </table>" +

		    "    <p style='margin-top: 30px; font-size: 16px; color: #424242;'>Your order will be processed and shipped within <strong>2-3 business days</strong>. We will update you once your order is dispatched.</p>" +
		    "    <p style='font-size: 16px; color: #424242;'>For any questions, please contact our support at <a href='mailto:support@agritradehub.com' style='color: #388e3c; font-weight: 600;'>support@agritradehub.com</a> or call <strong>+91-9876543210</strong>.</p>" +
		    "    <br>" +
		    "    <p style='font-size: 16px; color: #2e7d32; font-weight: 700;'>Warm regards,<br>Team AgriTrade Hub 🌱</p>" +
		    "  </div>" +
		    "</body>" +
		    "</html>";

		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
			helper.setTo(order.getMerchant().getEmail());
			helper.setSubject(subject);
			helper.setText(message, true);  // enable html content
			mailSender.send(mimeMessage);
		} catch (Exception e) {
		 System.err.println("error"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void SendBulkOrderPlacedEmail(Order order) {
	    try {
	        String recipient = order.getFpo() != null ? order.getFpo().getEmail() : null;
	        if (recipient == null || recipient.isBlank()) return;
	        BigDecimal total = order.getPricePerUnit().multiply(BigDecimal.valueOf(order.getQuantity()));
	        String subject = "📦 AgriTrade Hub: New Bulk Order Requires Confirmation";
	        String message = "<html><body style='font-family:Arial,sans-serif;padding:20px;'>"
	                + "<h2 style='color:#2e7d32;'>New Bulk Order</h2>"
	                + "<p>Hello <strong>" + order.getFpo().getName() + "</strong>,</p>"
	                + "<p>A merchant has placed a bulk order and payment has been completed.</p>"
	                + "<table style='border-collapse:collapse;width:100%;max-width:600px;'>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Merchant</td><td style='padding:8px;border:1px solid #ddd;'>" + order.getMerchant().getName() + "</td></tr>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Product</td><td style='padding:8px;border:1px solid #ddd;'>" + order.getProductName() + "</td></tr>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Quantity</td><td style='padding:8px;border:1px solid #ddd;'>" + order.getQuantity() + "</td></tr>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Total</td><td style='padding:8px;border:1px solid #ddd;'>₹" + total + "</td></tr>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Order ID</td><td style='padding:8px;border:1px solid #ddd;'>" + order.getOrderId() + "</td></tr>"
	                + "</table><p>Please open your FPO Orders page and confirm or cancel this order.</p>"
	                + "<p>Team AgriTrade Hub 🌾</p></body></html>";
	        MimeMessage mimeMessage = mailSender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
	        helper.setTo(recipient);
	        helper.setSubject(subject);
	        helper.setText(message, true);
	        mailSender.send(mimeMessage);
	    } catch (Exception e) {
	        System.err.println("Error sending bulk order notification: " + e.getMessage());
	    }
	}

	public void SendBulkOrderStatusEmail(Order order, String status) {
	    try {
	        if (order.getMerchant() == null) return;
	        BigDecimal total = order.getPricePerUnit().multiply(BigDecimal.valueOf(order.getQuantity()));
	        String subject = "Confirmed".equalsIgnoreCase(status)
	                ? "✅ AgriTrade Hub: Your Bulk Order Has Been Confirmed"
	                : "Cancelled".equalsIgnoreCase(status)
	                    ? "❌ AgriTrade Hub: Your Bulk Order Has Been Cancelled"
	                    : "📦 AgriTrade Hub: Bulk Order Status Updated";
	        String message = "<html><body style='font-family:Arial,sans-serif;padding:20px;'>"
	                + "<h2 style='color:#2e7d32;'>Bulk Order Status: " + status + "</h2>"
	                + "<p>Hello <strong>" + order.getMerchant().getName() + "</strong>,</p>"
	                + "<p>Your bulk order has been updated by <strong>" + (order.getFpo() != null ? order.getFpo().getName() : "the FPO") + "</strong>.</p>"
	                + "<table style='border-collapse:collapse;width:100%;max-width:600px;'>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Order ID</td><td style='padding:8px;border:1px solid #ddd;'>" + order.getOrderId() + "</td></tr>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Product</td><td style='padding:8px;border:1px solid #ddd;'>" + order.getProductName() + "</td></tr>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Quantity</td><td style='padding:8px;border:1px solid #ddd;'>" + order.getQuantity() + "</td></tr>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Total</td><td style='padding:8px;border:1px solid #ddd;'>₹" + total + "</td></tr>"
	                + "<tr><td style='padding:8px;border:1px solid #ddd;'>Status</td><td style='padding:8px;border:1px solid #ddd;'><strong>" + status + "</strong></td></tr>"
	                + "</table>"
	                + ("Confirmed".equalsIgnoreCase(status)
	                    ? "<p>Your paid bulk order is now confirmed and will be processed by the FPO.</p>"
	                    : "Cancelled".equalsIgnoreCase(status)
	                        ? "<p>The order was cancelled. If applicable, the payment refund has been initiated.</p>"
	                        : "")
	                + "<p>Team AgriTrade Hub 🌾</p></body></html>";
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(order.getMerchant().getEmail());
        helper.setSubject(subject);
        helper.setText(message, true);
        mailSender.send(mimeMessage);
	    } catch (Exception e) {
	        System.err.println("Error sending bulk order status email: " + e.getMessage());
	    }
	}

	public void SendOrderCancellationEmail(Order order, Payment payment) {
	    // Calculate total price
	    BigDecimal totalPrice = order.getPricePerUnit().multiply(new BigDecimal(order.getQuantity()));

	    String subject = "❌ AgriTrade Hub: Your Order Has Been Cancelled - Refund Initiated";
	    String message = "<html>" +
	        "<body style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; background-color: #fff3e0; margin: 0; padding: 20px;'>" +
	        "  <div style='max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 15px rgba(255,152,0,0.2); padding: 30px;'>" +
	        "    <h1 style='color: #e65100; border-bottom: 3px solid #ffb74d; padding-bottom: 12px; margin-bottom: 30px;'>Order Cancellation</h1>" +
	        "    <p style='font-size: 16px; color: #424242;'>Hello <strong>" + order.getMerchant().getName() + "</strong>,</p>" +
	        "    <p style='font-size: 16px; color: #424242;'>We regret to inform you that your order for <strong>" + order.getProductName() + "</strong> has been <span style='color:#d84315; font-weight: bold;'>cancelled</span>.</p>" +
	        "    <h2 style='color: #bf360c; margin-top: 35px;'>Order Summary</h2>" +
	        "    <table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>" +
	        "      <thead>" +
	        "        <tr style='background-color: #ffe0b2;'>" +
	        "          <th style='padding: 14px; border: 1px solid #ffcc80; text-align: left;'>Product Name</th>" +
	        "          <th style='padding: 14px; border: 1px solid #ffcc80; text-align: left;'>Quantity</th>" +
	        "          <th style='padding: 14px; border: 1px solid #ffcc80; text-align: left;'>Price (₹)</th>" +
	        "        </tr>" +
	        "      </thead>" +
	        "      <tbody>" +
	        "        <tr>" +
	        "          <td style='padding: 14px; border: 1px solid #ffe0b2;'>" + order.getProductName() + "</td>" +
	        "          <td style='padding: 14px; border: 1px solid #ffe0b2;'>" + order.getQuantity() + "</td>" +
	        "          <td style='padding: 14px; border: 1px solid #ffe0b2;'>₹" + order.getPricePerUnit() + "</td>" +
	        "        </tr>" +
	        "        <tr style='background-color: #fff8e1; font-weight: 700;'>" +
	        "          <td colspan='2' style='padding: 14px; border: 1px solid #ffcc80; text-align: right;'>Total Amount</td>" +
	        "          <td style='padding: 14px; border: 1px solid #ffcc80;'>₹" + totalPrice + "</td>" +
	        "        </tr>" +
	        "      </tbody>" +
	        "    </table>" +

	        "    <h2 style='color: #bf360c; margin-top: 35px;'>Refund Details</h2>" +
	        "    <table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>" +
	        "      <tbody>" +
	        "        <tr style='background-color: #ffe0b2;'>" +
	        "          <th style='padding: 12px; border: 1px solid #ffcc80; text-align: left;'>Payment Mode</th>" +
	        "          <td style='padding: 12px; border: 1px solid #ffe0b2;'>" + payment.getPaymentMode() + "</td>" +
	        "        </tr>" +
	        "        <tr style='background-color: #fff3e0;'>" +
	        "          <th style='padding: 12px; border: 1px solid #ffcc80; text-align: left;'>Transaction ID</th>" +
	        "          <td style='padding: 12px; border: 1px solid #ffe0b2;'>" + payment.getTransactionId() + "</td>" +
	        "        </tr>" +
	        "        <tr style='background-color: #ffe0b2;'>" +
	        "          <th style='padding: 12px; border: 1px solid #ffcc80; text-align: left;'>Amount</th>" +
	        "          <td style='padding: 12px; border: 1px solid #ffe0b2;'>₹" + payment.getAmount() + "</td>" +
	        "        </tr>" +
	        "        <tr style='background-color: #fff3e0;'>" +
	        "          <th style='padding: 12px; border: 1px solid #ffcc80; text-align: left;'>Payment Date</th>" +
	        "          <td style='padding: 12px; border: 1px solid #ffe0b2;'>" + payment.getPaymentDate().toLocalDate() + "</td>" +
	        "        </tr>" +
	        "      </tbody>" +
	        "    </table>" +

	        "    <p style='margin-top: 30px; font-size: 16px; color: #424242;'>We’ve initiated your refund process. The amount will be refunded to your original payment method within <strong>72 hours</strong>.</p>" +
	        "    <p style='font-size: 16px; color: #424242;'>If you have any questions, contact our support at <a href='mailto:support@agritradehub.com' style='color: #e65100; font-weight: 600;'>support@agritradehub.com</a> or call <strong>+91-9876543210</strong>.</p>" +
	        "    <br>" +
	        "    <p style='font-size: 16px; color: #e65100; font-weight: 700;'>We hope to serve you better next time.<br>Team AgriTrade Hub 🌾</p>" +
	        "  </div>" +
	        "</body>" +
	        "</html>";

	    try {
	        MimeMessage mimeMessage = mailSender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
	        helper.setTo(order.getMerchant().getEmail());
	        helper.setSubject(subject);
	        helper.setText(message, true); // HTML content
	        mailSender.send(mimeMessage);
	    } catch (Exception e) {
	        System.err.println("Error sending cancellation email: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	
	public void sendApprovalEmail(Object user) {
	    String name = "";
	    String email = "";
	    String userType = "";
	    String login = "";

	    if (user instanceof Merchant) {
	        Merchant merchant = (Merchant) user;
	        name = merchant.getName();
	        email = merchant.getEmail();
	        userType = "Merchant";
	        login = "Mlogin";
	    } else if (user instanceof Formers) {
	        Formers farmer = (Formers) user;
	        name = farmer.getName();
	        email = farmer.getEmail();
	        userType = "Farmer";
	        login ="Flogin";
	    } else {
	        throw new IllegalArgumentException("Unsupported user type.");
	    }

	    String subject = "✅ AgriTrade Hub - " + userType + " Account Approved";
	    String message = "<html>" +
	        "<body style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; background-color: #f1f8e9; margin: 0; padding: 20px;'>" +
	        "  <div style='max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 15px rgba(76,175,80,0.2); padding: 30px;'>" +
	        "    <h2 style='color: #388e3c;'>Congratulations, " + name + "!</h2>" +
	        "    <p style='font-size: 16px; color: #424242;'>Your " + userType.toLowerCase() + " account has been <strong>successfully approved</strong> by AgriTrade Hub Admin.</p>" +
	        "    <p style='font-size: 16px; color: #424242;'>You can now log in using your registered email and password.</p>" +
	        "    <div style='margin: 30px 0; padding: 20px; background-color: #e8f5e9; border-left: 5px solid #43a047;'>" +
	        "      <p style='font-size: 16px; color: #2e7d32;'><strong>Email:</strong> " + email + "<br>" +
	        "      <strong>Login Portal:</strong> <a href='http://192.168.0.141:8181/"+login+"' style='color: #2e7d32; font-weight: 600; font-size:15px;'>http://192.168.0.141:8181/"+login+"</a></p>" +
	        "    </div>" +
	        "    <p style='font-size: 15px; color: #616161;'>If you face any issues, contact support at <a href='mailto:support@agritradehub.com'>support@agritradehub.com</a></p>" +
	        "    <br><p style='font-size: 16px; color: #2e7d32;'>Best Regards,<br>Team AgriTrade Hub 🌾</p>" +
	        "  </div>" +
	        "</body>" +
	        "</html>";

	    try {
	        MimeMessage mimeMessage = mailSender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
	        helper.setTo(email);
	        helper.setSubject(subject);
	        helper.setText(message, true);  // HTML content
	        mailSender.send(mimeMessage);
	    } catch (Exception e) {
	        System.err.println("Failed to send approval email: " + e.getMessage());
	        e.printStackTrace();
	    }
	}




    public void SendFPOContributionCommittedEmail(FPO fpo, Formers farmer, Product product) {
        try {
            if (fpo == null || farmer == null || product == null || fpo.getEmail() == null) return;
            String subject = "🌾 AgriTrade Hub: New FPO Produce Contribution from " + farmer.getName();
            String message = "<html><body style='font-family:Arial,sans-serif;padding:20px;'>"
                    + "<h2 style='color:#2e7d32;'>New FPO Produce Contribution</h2>"
                    + "<p>Hello <strong>" + fpo.getName() + "</strong>,</p>"
                    + "<p>" + farmer.getName() + " has committed produce to your FPO.</p>"
                    + "<table style='border-collapse:collapse;width:100%;max-width:600px;'>"
                    + "<tr><td style='padding:8px;border:1px solid #ddd;'>Farmer</td><td style='padding:8px;border:1px solid #ddd;'>" + farmer.getName() + "</td></tr>"
                    + "<tr><td style='padding:8px;border:1px solid #ddd;'>Product</td><td style='padding:8px;border:1px solid #ddd;'>" + product.getProductName() + "</td></tr>"
                    + "<tr><td style='padding:8px;border:1px solid #ddd;'>Quantity</td><td style='padding:8px;border:1px solid #ddd;'>" + product.getQuantity() + "</td></tr>"
                    + "<tr><td style='padding:8px;border:1px solid #ddd;'>Mode</td><td style='padding:8px;border:1px solid #ddd;'>FPO Contribution</td></tr>"
                    + "</table>"
                    + "<p>The produce will remain hidden from the independent merchant Products listing until it is included in an FPO bulk lot.</p>"
                    + "<p>Team AgriTrade Hub 🌾</p></body></html>";
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(fpo.getEmail());
            helper.setSubject(subject);
            helper.setText(message, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Error sending FPO contribution notification: " + e.getMessage());
        }
    }

    public void SendFPOFarmerAddedEmail(FPO fpo, Formers farmer) {
        if (fpo == null || farmer == null || farmer.getEmail() == null || farmer.getEmail().isBlank()) {
            return;
        }

        String subject = "🌾 AgriTrade Hub: You Have Been Added to an FPO";
        String message = "<html><body style='font-family:Segoe UI,Tahoma,Arial,sans-serif;background:#f1f8e9;padding:20px;'>"
                + "<div style='max-width:620px;margin:20px auto;background:#fff;border-radius:12px;padding:30px;box-shadow:0 4px 15px rgba(46,125,50,.15);'>"
                + "<h2 style='color:#2e7d32;'>FPO Membership Notification</h2>"
                + "<p>Hello <strong>" + farmer.getName() + "</strong>,</p>"
                + "<p>You have been added as a verified farmer member of <strong>" + fpo.getName() + "</strong> on AgriTrade Hub.</p>"
                + "<table style='border-collapse:collapse;width:100%;max-width:560px;'>"
                + "<tr><td style='padding:9px;border:1px solid #c8e6c9;'>FPO Name</td><td style='padding:9px;border:1px solid #c8e6c9;'>" + fpo.getName() + "</td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #c8e6c9;'>Registration No.</td><td style='padding:9px;border:1px solid #c8e6c9;'>" + fpo.getRegistrationNumber() + "</td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #c8e6c9;'>Contact Person</td><td style='padding:9px;border:1px solid #c8e6c9;'>" + fpo.getContactPerson() + "</td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #c8e6c9;'>Phone</td><td style='padding:9px;border:1px solid #c8e6c9;'>" + fpo.getPhone() + "</td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #c8e6c9;'>Location</td><td style='padding:9px;border:1px solid #c8e6c9;'>" + fpo.getDistrict() + ", " + fpo.getState() + "</td></tr>"
                + "</table>"
                + "<p style='margin-top:22px;'>The FPO may aggregate your produce for bulk marketplace orders. When your produce is included in a paid bulk order, AgriTrade Hub will notify you of the quantity attributed to you and the corresponding amount.</p>"
                + "<p style='color:#2e7d32;font-weight:700;'>Team AgriTrade Hub 🌱</p>"
                + "</div></body></html>";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(farmer.getEmail());
            helper.setSubject(subject);
            helper.setText(message, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Error sending FPO farmer-added email: " + e.getMessage());
        }
    }

    public void SendBulkFarmerContributionEmail(Order order, OrderFarmerContribution allocation, String status) {
        if (order == null || allocation == null || allocation.getFarmer() == null
                || allocation.getFarmer().getEmail() == null || allocation.getFarmer().getEmail().isBlank()) {
            return;
        }

        String normalized = status == null ? "" : status.trim();
        String subject;
        String heading;
        String messageText;

        if ("Paid".equalsIgnoreCase(normalized)) {
            subject = "💰 AgriTrade Hub: Your Produce Is Included in a Paid Bulk Order";
            heading = "Bulk Sale Allocation Recorded";
            messageText = "Your produce has been allocated to a paid bulk order. The order is currently waiting for FPO confirmation.";
        } else if ("Confirmed".equalsIgnoreCase(normalized)) {
            subject = "✅ AgriTrade Hub: Your Bulk Produce Sale Has Been Confirmed";
            heading = "Bulk Sale Confirmed";
            messageText = "The FPO has confirmed the bulk order containing your produce.";
        } else if ("Cancelled".equalsIgnoreCase(normalized)) {
            subject = "❌ AgriTrade Hub: Bulk Order Containing Your Produce Was Cancelled";
            heading = "Bulk Sale Cancelled";
            messageText = "The bulk order containing your produce was cancelled and the buyer refund process was initiated.";
        } else if ("Delivered".equalsIgnoreCase(normalized)) {
            subject = "📦 AgriTrade Hub: Bulk Order Containing Your Produce Was Delivered";
            heading = "Bulk Order Delivered";
            messageText = "The bulk order containing your produce has been marked as delivered.";
        } else {
            subject = "📦 AgriTrade Hub: Bulk Order Update";
            heading = "Bulk Order Update";
            messageText = "There is an update for a bulk order containing your produce.";
        }

        String fpoName = order.getFpo() != null ? order.getFpo().getName() : "FPO";
        String merchantName = order.getMerchant() != null ? order.getMerchant().getName() : "Bulk Buyer";
        String message = "<html><body style='font-family:Segoe UI,Tahoma,Arial,sans-serif;background:#f5f8f5;padding:20px;'>"
                + "<div style='max-width:620px;margin:20px auto;background:#fff;border-radius:12px;padding:30px;box-shadow:0 4px 15px rgba(46,125,50,.15);'>"
                + "<h2 style='color:#2e7d32;'>" + heading + "</h2>"
                + "<p>Hello <strong>" + allocation.getFarmer().getName() + "</strong>,</p>"
                + "<p>" + messageText + "</p>"
                + "<table style='border-collapse:collapse;width:100%;max-width:560px;'>"
                + "<tr><td style='padding:9px;border:1px solid #ddd;'>FPO</td><td style='padding:9px;border:1px solid #ddd;'>" + fpoName + "</td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #ddd;'>Bulk Order ID</td><td style='padding:9px;border:1px solid #ddd;'>" + order.getOrderId() + "</td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #ddd;'>Buyer</td><td style='padding:9px;border:1px solid #ddd;'>" + merchantName + "</td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #ddd;'>Product</td><td style='padding:9px;border:1px solid #ddd;'>" + order.getProductName() + "</td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #ddd;'>Your Quantity</td><td style='padding:9px;border:1px solid #ddd;'><strong>" + allocation.getQuantity() + "</strong></td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #ddd;'>Your Amount</td><td style='padding:9px;border:1px solid #ddd;'><strong>₹" + allocation.getAmount() + "</strong></td></tr>"
                + "<tr><td style='padding:9px;border:1px solid #ddd;'>Order Status</td><td style='padding:9px;border:1px solid #ddd;'><strong>" + normalized + "</strong></td></tr>"
                + "</table>"
                + "<p style='margin-top:22px;color:#555;'>This notification gives you a clear record of how much of your produce was included in the bulk transaction.</p>"
                + "<p style='color:#2e7d32;font-weight:700;'>Team AgriTrade Hub 🌾</p>"
                + "</div></body></html>";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(allocation.getFarmer().getEmail());
            helper.setSubject(subject);
            helper.setText(message, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Error sending farmer bulk contribution email: " + e.getMessage());
        }
    }

}