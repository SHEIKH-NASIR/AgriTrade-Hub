package com.softpro.ATH.Controller;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Model.AdminInfo;
import com.softpro.ATH.Model.Dispute;
import com.softpro.ATH.Repository.AdminInfoRepo;
import com.softpro.ATH.Repository.DisputeRepo;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/enquiry/disputes")
public class AdminDisputeController {
    @Autowired private DisputeRepo disputeRepo;
    @Autowired private AdminInfoRepo adminInfoRepo;

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes attributes) {
        if (session.getAttribute("admin") == null) return "redirect:/adminlogin";
        AdminInfo admininfo = adminInfoRepo.findById(session.getAttribute("admin").toString()).orElse(null);
        if (admininfo == null) return "redirect:/adminlogin";
        Dispute dispute = disputeRepo.findById(id).orElse(null);
        if (dispute == null) { attributes.addFlashAttribute("error", "Dispute not found."); return "redirect:/admin/enquiry"; }
        model.addAttribute("admininfo", admininfo);
        model.addAttribute("dispute", dispute);
        model.addAttribute("active6", "active");
        return "admin/dispute-detail";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id, @RequestParam String resolution, HttpSession session, RedirectAttributes attributes) {
        if (session.getAttribute("admin") == null) return "redirect:/adminlogin";
        try {
            Dispute d = disputeRepo.findById(id).orElseThrow();
            if (resolution == null || resolution.isBlank()) throw new IllegalArgumentException("Resolution note is required.");
            d.setAdminResolution(resolution.trim()); d.setStatus("RESOLVED"); d.setResolvedAt(LocalDateTime.now());
            disputeRepo.save(d); attributes.addFlashAttribute("msg", "Dispute resolved successfully.");
        } catch (Exception e) { attributes.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/enquiry";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam String status, HttpSession session, RedirectAttributes attributes) {
        if (session.getAttribute("admin") == null) return "redirect:/adminlogin";
        try {
            Dispute d = disputeRepo.findById(id).orElseThrow();
            if (!status.equals("OPEN") && !status.equals("IN_REVIEW") && !status.equals("RESOLVED")) throw new IllegalArgumentException("Invalid status.");
            d.setStatus(status);
            if ("RESOLVED".equals(status)) d.setResolvedAt(LocalDateTime.now());
            disputeRepo.save(d); attributes.addFlashAttribute("msg", "Dispute status updated.");
        } catch (Exception e) { attributes.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/enquiry/disputes/" + id;
    }
}
