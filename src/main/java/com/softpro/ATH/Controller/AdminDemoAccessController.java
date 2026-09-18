package com.softpro.ATH.Controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.MerchantRepo;

@Controller
@RequestMapping("/Admin")
public class AdminDemoAccessController {

    @Autowired
    private MerchantRepo merchantRepo;

    @Autowired
    private FPORepo fpoRepo;

    @Autowired
    private FormersRepo formerRepo;

    /*
     * ============================================================
     * DEMO ACCESS MANAGEMENT
     * ============================================================
     *
     * Admin can selectively enable/disable Demo access for:
     * - Merchants
     * - FPOs
     * - Independent Farmers
     *
     * This does NOT create Demo Orders.
     * It only controls the demoEnabled permission flag.
     */

    @GetMapping("/DemoAccess")
    public String demoAccess(Model model) {

        model.addAttribute("merchants", merchantRepo.findAll());
        model.addAttribute("fpos", fpoRepo.findAll());
        model.addAttribute("farmers", formerRepo.findAll());

        return "Admin/demo-access";
    }

    /*
     * ============================================================
     * MERCHANT
     * ============================================================
     */

    @PostMapping("/DemoAccess/Merchant/{id}")
    public String toggleMerchantDemoAccess(
            @PathVariable Long id,
            @RequestParam("enabled") boolean enabled,
            RedirectAttributes attributes) {

        Optional<Merchant> optionalMerchant =
                merchantRepo.findById(id);

        if (optionalMerchant.isEmpty()) {

            attributes.addFlashAttribute(
                    "error",
                    "Merchant not found."
            );

            return "redirect:/Admin/DemoAccess";
        }

        Merchant merchant = optionalMerchant.get();

        merchant.setDemoEnabled(enabled);

        merchantRepo.save(merchant);

        attributes.addFlashAttribute(
                "success",
                enabled
                        ? "Demo access enabled for merchant."
                        : "Demo access disabled for merchant."
        );

        return "redirect:/Admin/DemoAccess";
    }

    /*
     * ============================================================
     * FPO
     * ============================================================
     */

    @PostMapping("/DemoAccess/FPO/{id}")
    public String toggleFpoDemoAccess(
            @PathVariable Long id,
            @RequestParam("enabled") boolean enabled,
            RedirectAttributes attributes) {

        Optional<FPO> optionalFpo =
                fpoRepo.findById(id);

        if (optionalFpo.isEmpty()) {

            attributes.addFlashAttribute(
                    "error",
                    "FPO not found."
            );

            return "redirect:/Admin/DemoAccess";
        }

        FPO fpo = optionalFpo.get();

        fpo.setDemoEnabled(enabled);

        fpoRepo.save(fpo);

        attributes.addFlashAttribute(
                "success",
                enabled
                        ? "Demo access enabled for FPO."
                        : "Demo access disabled for FPO."
        );

        return "redirect:/Admin/DemoAccess";
    }

    /*
     * ============================================================
     * INDEPENDENT FARMER
     * ============================================================
     */

    @PostMapping("/DemoAccess/Farmer/{id}")
    public String toggleFarmerDemoAccess(
            @PathVariable Long id,
            @RequestParam("enabled") boolean enabled,
            RedirectAttributes attributes) {

        Optional<Formers> optionalFarmer =
                formerRepo.findById(id);

        if (optionalFarmer.isEmpty()) {

            attributes.addFlashAttribute(
                    "error",
                    "Independent farmer not found."
            );

            return "redirect:/Admin/DemoAccess";
        }

        Formers farmer = optionalFarmer.get();

        farmer.setDemoEnabled(enabled);

        formerRepo.save(farmer);

        attributes.addFlashAttribute(
                "success",
                enabled
                        ? "Demo access enabled for farmer."
                        : "Demo access disabled for farmer."
        );

        return "redirect:/Admin/DemoAccess";
    }
}