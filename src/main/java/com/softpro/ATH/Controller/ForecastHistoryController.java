package com.softpro.ATH.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Model.AdminInfo;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.ForecastDemandHistory;
import com.softpro.ATH.Repository.AdminInfoRepo;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.ForecastDemandHistoryRepo;
import com.softpro.ATH.Service.ForecastHistoryService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/forecast-history")
public class ForecastHistoryController {

    @Autowired
    private ForecastHistoryService historyService;

    @Autowired
    private ForecastDemandHistoryRepo historyRepo;

    @Autowired
    private AdminInfoRepo adRepo;

    @Autowired
    private FormersRepo formersRepo;

    @Autowired
    private FPORepo fpoRepo;


    // =========================================================
    // AI DEMAND FORECAST / HISTORICAL DATA PAGE
    // =========================================================

    @GetMapping
    public String showForecastHistory(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        // -----------------------------------------------------
        // CHECK ADMIN LOGIN
        // -----------------------------------------------------

        if (session.getAttribute("admin") == null) {

            redirectAttributes.addFlashAttribute(
                    "msg",
                    "Session Expired 👾"
            );

            return "redirect:/adminlogin";
        }


        // -----------------------------------------------------
        // LOAD FARMERS
        // -----------------------------------------------------

        List<Formers> farmers =
                formersRepo.findAll();


        // -----------------------------------------------------
        // LOAD FPOs
        // -----------------------------------------------------

        List<FPO> fpos =
                fpoRepo.findAll();


        // -----------------------------------------------------
        // LOAD HISTORICAL DEMAND
        // -----------------------------------------------------

        List<ForecastDemandHistory> history =
                historyRepo.findAllByOrderByDemandDateDesc();


        // -----------------------------------------------------
        // SEND DATA TO THYMELEAF
        // -----------------------------------------------------

        model.addAttribute(
                "farmers",
                farmers
        );

        model.addAttribute(
                "fpos",
                fpos
        );

        model.addAttribute(
                "history",
                history
        );

        model.addAttribute(
                "activeForecastHistory",
                "active"
        );


        // -----------------------------------------------------
        // LOAD LOGGED-IN ADMIN
        // -----------------------------------------------------

        AdminInfo admininfo =
                adRepo.findById(
                        session.getAttribute("admin").toString()
                ).orElse(null);

        if (admininfo != null) {
            model.addAttribute(
                    "admininfo",
                    admininfo
            );
        }


        return "admin/forecast-history";
    }


    // =========================================================
    // SAVE HISTORICAL DEMAND
    // =========================================================

    @PostMapping("/save")
    public String saveHistoricalDemand(

            HttpSession session,

            @RequestParam("scopeType")
            String scopeType,

            @RequestParam(
                    value = "scopeId",
                    required = false)
            Long scopeId,

            @RequestParam("productName")
            String productName,

            @RequestParam("demandDate")
            LocalDate demandDate,

            @RequestParam("quantity")
            int quantity,

            @RequestParam(
                    value = "pricePerUnit",
                    required = false)
            BigDecimal pricePerUnit,

            RedirectAttributes redirectAttributes) {


        // -----------------------------------------------------
        // CHECK ADMIN LOGIN
        // -----------------------------------------------------

        if (session.getAttribute("admin") == null) {

            redirectAttributes.addFlashAttribute(
                    "msg",
                    "Session Expired 👾"
            );

            return "redirect:/adminlogin";
        }


        try {

            // -------------------------------------------------
            // BASIC VALIDATION
            // -------------------------------------------------

            if (scopeType == null
                    || scopeType.isBlank()) {

                throw new IllegalArgumentException(
                        "Please select Farmer or FPO."
                );
            }


            if (scopeId == null) {

                throw new IllegalArgumentException(
                        "Please select the Farmer/FPO."
                );
            }


            if (productName == null
                    || productName.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Product name is required."
                );
            }


            if (demandDate == null) {

                throw new IllegalArgumentException(
                        "Demand date is required."
                );
            }


            if (quantity <= 0) {

                throw new IllegalArgumentException(
                        "Demand quantity must be greater than 0."
                );
            }


            // -------------------------------------------------
            // SAVE
            // -------------------------------------------------

            historyService.saveHistory(

                    scopeType,

                    scopeId,

                    productName.trim(),

                    demandDate,

                    quantity,

                    pricePerUnit
            );


            redirectAttributes.addFlashAttribute(
                    "success",
                    "Historical demand added successfully. "
                    + "AI will use it in the next forecast."
            );


        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Unable to save historical demand."
            );
        }


        return "redirect:/admin/forecast-history";
    }


    // =========================================================
    // DELETE HISTORICAL DEMAND
    // =========================================================

    @GetMapping("/delete")
    public String deleteHistoricalDemand(

            HttpSession session,

            @RequestParam("id")
            Long id,

            RedirectAttributes redirectAttributes) {


        // -----------------------------------------------------
        // CHECK ADMIN LOGIN
        // -----------------------------------------------------

        if (session.getAttribute("admin") == null) {

            redirectAttributes.addFlashAttribute(
                    "msg",
                    "Session Expired 👾"
            );

            return "redirect:/adminlogin";
        }


        try {

            if (id == null) {

                throw new IllegalArgumentException(
                        "Invalid historical demand ID."
                );
            }


            historyService.deleteHistory(id);


            redirectAttributes.addFlashAttribute(
                    "success",
                    "Historical demand record deleted."
            );


        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Unable to delete historical demand."
            );
        }


        return "redirect:/admin/forecast-history";
    }

}