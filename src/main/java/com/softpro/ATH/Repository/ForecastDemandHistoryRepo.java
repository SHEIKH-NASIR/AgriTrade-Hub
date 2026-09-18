package com.softpro.ATH.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.ForecastDemandHistory;

@Repository
public interface ForecastDemandHistoryRepo
        extends JpaRepository<ForecastDemandHistory, Long> {

    List<ForecastDemandHistory>
    findByFarmerOrderByDemandDateAsc(Formers farmer);

    List<ForecastDemandHistory>
    findByFpoOrderByDemandDateAsc(FPO fpo);

    List<ForecastDemandHistory>
    findAllByOrderByDemandDateDesc();
}