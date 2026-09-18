package com.softpro.ATH.ai;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping({"/DemandForecast", "/demand-forecast"})
public class DemandForecastController {

    @Autowired
    private DemandForecastService demandForecastService;

    @Autowired
    private FormersRepo formersRepo;

    @Autowired
    private FPORepo fpoRepo;

    @GetMapping("/Former")
    public String farmerForecast(HttpSession session, Model model) {
        Formers farmer = getLoggedInFarmer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        List<DemandForecastResponse> forecasts =
                demandForecastService.forecastForFarmer(farmer);

        model.addAttribute("former", farmer);
        model.addAttribute("forecasts", forecasts);
        model.addAttribute("activeDemandForecast", "active");

        return "Former/demand-forecast";
    }

    @GetMapping("/FPO")
    public String fpoForecast(HttpSession session, Model model) {
        FPO fpo = getLoggedInFpo(session);

        if (fpo == null) {
            return "redirect:/FPO/Login";
        }

        List<DemandForecastResponse> forecasts =
                demandForecastService.forecastForFpo(fpo);

        model.addAttribute("fpo", fpo);
        model.addAttribute("forecasts", forecasts);
        model.addAttribute("activeDemandForecast", "active");

        return "FPO/demand-forecast";
    }

    private Formers getLoggedInFarmer(HttpSession session) {
        Object farmerId = session.getAttribute("loggedInFormerId");

        if (farmerId != null) {
            try {
                return formersRepo.findById(Long.parseLong(farmerId.toString()))
                        .orElse(null);
            } catch (Exception ignored) {
            }
        }

        Object farmer = session.getAttribute("loggedInFormer");
        return farmer instanceof Formers ? (Formers) farmer : null;
    }

    private FPO getLoggedInFpo(HttpSession session) {
        Object fpoId = session.getAttribute("loggedInFPOId");

        if (fpoId == null) {
            return null;
        }

        try {
            return fpoRepo.findById(Long.parseLong(fpoId.toString()))
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
