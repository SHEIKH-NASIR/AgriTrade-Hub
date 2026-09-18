package com.softpro.ATH.Service;

import java.util.List;

import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Review;
import com.softpro.ATH.Repository.ReviewRepo;

@Service("reviewService")
public class ReviewService {

    @Autowired
    private ReviewRepo reviewRepo;

    public double averageForFarmer(Long id) {
        Double value = reviewRepo.averageForFarmer(id);
        return value == null ? 0.0 : Math.round(value * 10.0) / 10.0;
    }

    public double averageForFpo(Long id) {
        Double value = reviewRepo.averageForFpo(id);
        return value == null ? 0.0 : Math.round(value * 10.0) / 10.0;
    }

    public boolean hasReview(Order order, Merchant merchant) { return reviewRepo.findByOrderAndMerchant(order, merchant).isPresent(); }

    public long countForFarmer(Formers farmer) { return reviewRepo.countByFarmer(farmer); }
    public long countForFpo(FPO fpo) { return reviewRepo.countByFpo(fpo); }
    public List<Review> farmerReviews(Formers farmer) { return reviewRepo.findByFarmerOrderByCreatedAtDesc(farmer); }
    public List<Review> fpoReviews(FPO fpo) { return reviewRepo.findByFpoOrderByCreatedAtDesc(fpo); }
}
