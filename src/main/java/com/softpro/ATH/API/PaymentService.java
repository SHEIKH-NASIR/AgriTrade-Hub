package com.softpro.ATH.API;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class PaymentService {

    private RazorpayClient razorpayClient;

    public PaymentService(
            @Value("${razorpay.key_id}") String keyId,
            @Value("${razorpay.key_secret}") String keySecret) throws Exception {
        this.razorpayClient = new RazorpayClient("rzp_live_Io1s9ctQtD0G1b", "HO60ThPu65xyvH7ewH5eVcWp");
    }

    public Order createRazorpayOrder(int amount) throws Exception {
        return createRazorpayOrder(BigDecimal.valueOf(amount));
    }

    public Order createRazorpayOrder(BigDecimal amount) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        long paise = amount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact();
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", paise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());
        return razorpayClient.orders.create(orderRequest);
    }
    
    
    public Refund refundPayment(String paymentId) throws RazorpayException {
        JSONObject refundRequest = new JSONObject();
        refundRequest.put("payment_id", paymentId);

        // Optional: You can also refund partially with amount
        // refundRequest.put("amount", 5000); // Amount in paise

        return razorpayClient.payments.refund(refundRequest);
    }
}
