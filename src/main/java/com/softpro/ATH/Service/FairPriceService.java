package com.softpro.ATH.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.softpro.ATH.Dto.FairPriceResponse;
import com.softpro.ATH.Model.MarketPriceReference;
import com.softpro.ATH.Repository.MarketPriceReferenceRepo;

@Service
public class FairPriceService {

    private final MarketPriceReferenceRepo
            marketPriceReferenceRepo;

    public FairPriceService(
            MarketPriceReferenceRepo marketPriceReferenceRepo) {

        this.marketPriceReferenceRepo =
                marketPriceReferenceRepo;
    }

    public FairPriceResponse checkPrice(
            String productName,
            String category,
            BigDecimal sellerPrice) {

        FairPriceResponse response =
                new FairPriceResponse();

        if (productName == null
                || productName.isBlank()) {

            response.setStatus("INVALID");
            response.setMessage(
                    "Product name is required."
            );

            return response;
        }

        if (sellerPrice == null
                || sellerPrice.compareTo(BigDecimal.ZERO) <= 0) {

            response.setStatus("INVALID");
            response.setMessage(
                    "Seller price must be greater than zero."
            );

            return response;
        }

        Optional<MarketPriceReference> reference =
                Optional.empty();

        if (category != null
                && !category.isBlank()) {

            reference =
                    marketPriceReferenceRepo
                            .findTopByProductNameIgnoreCaseAndCategoryIgnoreCaseOrderByUpdatedAtDesc(
                                    productName.trim(),
                                    category.trim()
                            );
        }

        if (reference.isEmpty()) {

            reference =
                    marketPriceReferenceRepo
                            .findTopByProductNameIgnoreCaseOrderByUpdatedAtDesc(
                                    productName.trim()
                            );
        }

        if (reference.isEmpty()) {

            response.setReferenceAvailable(false);
            response.setFair(false);
            response.setStatus("NO_REFERENCE");
            response.setSellerPrice(sellerPrice);

            response.setMessage(
                    "No market price reference is available for this product."
            );

            return response;
        }

        MarketPriceReference market =
                reference.get();

        BigDecimal marketPrice =
                market.getReferencePrice();

        BigDecimal lower =
                marketPrice.multiply(
                        BigDecimal.ONE.subtract(
                                BigDecimal.valueOf(
                                        market.getLowerPercentage()
                                                / 100.0
                                )
                        )
                ).setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal upper =
                marketPrice.multiply(
                        BigDecimal.ONE.add(
                                BigDecimal.valueOf(
                                        market.getUpperPercentage()
                                                / 100.0
                                )
                        )
                ).setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        boolean fair =
                sellerPrice.compareTo(lower) >= 0
                        && sellerPrice.compareTo(upper) <= 0;

        response.setReferenceAvailable(true);
        response.setFair(fair);

        response.setReferencePrice(marketPrice);
        response.setLowerFairPrice(lower);
        response.setUpperFairPrice(upper);
        response.setSellerPrice(sellerPrice);

        if (fair) {

            response.setStatus("FAIR");

            response.setMessage(
                    "Seller price is within the fair market range."
            );

        } else if (sellerPrice.compareTo(upper) > 0) {

            response.setStatus("ABOVE_MARKET");

            response.setMessage(
                    "Seller price is above the suggested fair market range."
            );

        } else {

            response.setStatus("BELOW_MARKET");

            response.setMessage(
                    "Seller price is below the suggested fair market range."
            );
        }

        return response;
    }
}