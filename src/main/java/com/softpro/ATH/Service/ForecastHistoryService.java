package com.softpro.ATH.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.ForecastDemandHistory;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.ForecastDemandHistoryRepo;

@Service
public class ForecastHistoryService {

    @Autowired
    private ForecastDemandHistoryRepo historyRepo;

    @Autowired
    private FormersRepo formersRepo;

    @Autowired
    private FPORepo fpoRepo;


    /**
     * Save historical demand for an independent farmer
     * or an FPO.
     */
    public ForecastDemandHistory saveHistory(
            String scopeType,
            Long scopeId,
            String productName,
            LocalDate demandDate,
            int quantity,
            BigDecimal pricePerUnit) {

        if (scopeType == null
                || productName == null
                || productName.isBlank()
                || demandDate == null
                || quantity <= 0) {

            throw new IllegalArgumentException(
                    "Invalid historical demand data."
            );
        }

        ForecastDemandHistory history =
                new ForecastDemandHistory();

        history.setProductName(
                productName.trim()
        );

        history.setDemandDate(
                demandDate
        );

        history.setQuantity(
                quantity
        );

        history.setPricePerUnit(
                pricePerUnit
        );

        history.setSource(
                "HISTORICAL"
        );


        if ("FARMER".equalsIgnoreCase(scopeType)) {

            Formers farmer =
                    formersRepo.findById(scopeId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Farmer not found."
                                    )
                            );

            history.setFarmer(farmer);
            history.setFpo(null);

        } else if ("FPO".equalsIgnoreCase(scopeType)) {

            FPO fpo =
                    fpoRepo.findById(scopeId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "FPO not found."
                                    )
                            );

            history.setFpo(fpo);
            history.setFarmer(null);

        } else {

            throw new IllegalArgumentException(
                    "Invalid forecast scope."
            );
        }


        return historyRepo.save(history);
    }


    public void deleteHistory(Long id) {

        if (id == null) {
            return;
        }

        historyRepo.deleteById(id);
    }
}