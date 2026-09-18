package com.softpro.ATH.Controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Review;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.ReviewRepo;
import com.softpro.ATH.Service.ReviewService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Merchant")
public class ReviewController {

    @Autowired private OrderRepo orderRepo;
    @Autowired private ReviewRepo reviewRepo;
    @Autowired private FormersRepo formersRepo;
    @Autowired private FPORepo fpoRepo;
    @Autowired private ReviewService reviewService;

    @PostMapping("/Reviews/{orderId}")
    @Transactional
    public String submitReview(@PathVariable Long orderId,
                               @RequestParam int rating,
                               @RequestParam(required = false) Integer qualityRating,
                               @RequestParam(required = false) Integer quantityAccuracyRating,
                               @RequestParam(required = false) Integer timelinessRating,
                               @RequestParam(required = false) String comment,
                               HttpSession session,
                               RedirectAttributes attributes) {
        Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
        if (merchant == null) return "redirect:/Mlogin";
        try {
            Order order = orderRepo.findByOrderId(orderId);
            if (order == null || order.getMerchant() == null || order.getMerchant().getId() != merchant.getId())
                throw new IllegalArgumentException("Order not found.");
            if (!("Delivered".equalsIgnoreCase(order.getOrderStatus()) || order.getDeliveredDate() != null))
                throw new IllegalArgumentException("Review is available only after delivery.");
            if (reviewRepo.findByOrderAndMerchant(order, merchant).isPresent())
                throw new IllegalArgumentException("You have already reviewed this order.");
            if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be between 1 and 5.");

            Review review = new Review();
            review.setOrder(order);
            review.setMerchant(merchant);
            review.setRating(rating);
            review.setQualityRating(validOptionalRating(qualityRating));
            review.setQuantityAccuracyRating(validOptionalRating(quantityAccuracyRating));
            review.setTimelinessRating(validOptionalRating(timelinessRating));
            review.setComment(comment == null ? null : comment.trim());
            review.setCreatedAt(LocalDateTime.now());

            if (order.getFarmer() != null) {
                review.setFarmer(order.getFarmer());
                review.setSellerType("FARMER");
            } else if (order.getFpo() != null) {
                review.setFpo(order.getFpo());
                review.setSellerType("FPO");
            } else {
                throw new IllegalArgumentException("Seller information is missing from this order.");
            }
            reviewRepo.save(review);
            attributes.addFlashAttribute("success", "Review submitted successfully.");
        } catch (Exception e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/Merchant/MyOrders";
    }

    private Integer validOptionalRating(Integer value) {
        return value != null && value >= 1 && value <= 5 ? value : null;
    }

    /*
     * =========================================================
     * FARMER / FPO REVIEW INBOX
     * =========================================================
     * Sellers can see reviews submitted by merchants for their
     * own delivered orders. Existing merchant review flow is
     * unchanged.
     */

    @GetMapping("/Reviews/Farmer")
    public String farmerReviews(HttpSession session, Model model) {
        Formers farmer = (Formers) session.getAttribute("loggedInFormer");
        if (farmer == null) return "redirect:/formerlogin";

        model.addAttribute("farmer", farmer);
        model.addAttribute("reviews", reviewService.farmerReviews(farmer));
        model.addAttribute("averageRating", reviewService.averageForFarmer(farmer.getId()));
        model.addAttribute("reviewCount", reviewService.countForFarmer(farmer));
        model.addAttribute("activeReviews", "active");
        return "Former/reviews";
    }

    @GetMapping("/Reviews/FPO")
    public String fpoReviews(HttpSession session, Model model) {
        Object fpoIdObject = session.getAttribute("loggedInFPOId");
        if (fpoIdObject == null) return "redirect:/fpologin";

        try {
            Long fpoId = Long.parseLong(fpoIdObject.toString());
            FPO fpo = fpoRepo.findById(fpoId).orElseThrow();

            model.addAttribute("fpo", fpo);
            model.addAttribute("reviews", reviewService.fpoReviews(fpo));
            model.addAttribute("averageRating", reviewService.averageForFpo(fpo.getId()));
            model.addAttribute("reviewCount", reviewService.countForFpo(fpo));
            model.addAttribute("activeReviews", "active");
            return "FPO/reviews";
        } catch (NumberFormatException e) {
            return "redirect:/fpologin";
        }
    }


    @GetMapping("/Seller/Farmer/{id}")
    public String farmerProfile(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("loggedInMerchant") == null) return "redirect:/Mlogin";
        Formers farmer = formersRepo.findById(id).orElseThrow();
        model.addAttribute("sellerType", "Farmer");
        model.addAttribute("seller", farmer);
        model.addAttribute("averageRating", reviewService.averageForFarmer(id));
        model.addAttribute("reviewCount", reviewService.countForFarmer(farmer));
        model.addAttribute("reviews", reviewService.farmerReviews(farmer));
        return "Merchant/seller-profile";
    }

    @GetMapping("/Seller/FPO/{id}")
    public String fpoProfile(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("loggedInMerchant") == null) return "redirect:/Mlogin";
        FPO fpo = fpoRepo.findById(id).orElseThrow();
        model.addAttribute("sellerType", "FPO");
        model.addAttribute("seller", fpo);
        model.addAttribute("averageRating", reviewService.averageForFpo(id));
        model.addAttribute("reviewCount", reviewService.countForFpo(fpo));
        model.addAttribute("reviews", reviewService.fpoReviews(fpo));
        return "Merchant/seller-profile";
    }
}
