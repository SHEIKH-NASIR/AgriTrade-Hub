package com.softpro.ATH.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.softpro.ATH.Repository.DisputeRepo;

@ControllerAdvice
public class AdminEnquiryDisputeAdvice {
    @Autowired private DisputeRepo disputeRepo;

    @ModelAttribute
    public void addDisputesToEnquiry(org.springframework.ui.Model model, HttpServletRequest request) {
        if (request.getRequestURI().equals(request.getContextPath() + "/admin/enquiry")) {
            model.addAttribute("disputeList", disputeRepo.findAllByOrderByCreatedAtDesc());
        }
    }
}
