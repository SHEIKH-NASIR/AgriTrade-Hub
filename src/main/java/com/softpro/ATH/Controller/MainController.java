package com.softpro.ATH.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Dto.EnquiryDto;
import com.softpro.ATH.Dto.FPODto;
import com.softpro.ATH.Dto.FormersDto;
import com.softpro.ATH.Dto.MerchantDto;
import com.softpro.ATH.Model.AdminInfo;
import com.softpro.ATH.Model.Enquiry;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Repository.AdminInfoRepo;
import com.softpro.ATH.Repository.EnquiryRepo;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.MerchantRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    @Autowired
    EnquiryRepo eqRepo;

    @Autowired
    AdminInfoRepo adRepo;

    @Autowired
    FormersRepo formersRepo;

    @Autowired
    MerchantRepo merchantRepo;

    @Autowired
    FPORepo fpoRepo;


    // =========================================================
    // SHOW INDEX PAGE
    // =========================================================

    @GetMapping({"/", "/home"})
    public String ShowIndex(Model model) {

        model.addAttribute("active1", "active");

        return "index";
    }


    // =========================================================
    // ABOUT US
    // =========================================================

    @GetMapping("/aboutus")
    public String ShowAbout() {

        return "aboutus";
    }


    // =========================================================
    // SERVICES
    // =========================================================

    @GetMapping("/services")
    public String ShowServices() {

        return "services";
    }


    // =========================================================
    // ADMIN LOGIN
    // =========================================================

    @GetMapping("/adminlogin")
    public String ShowAdminLogin() {

        return "adminlogin";
    }


    @PostMapping("/adminlogin")
    public String AdminLogin(
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes attributes) {

        try {

            String userid = request.getParameter("userid");
            String password = request.getParameter("password");

            AdminInfo adinfo = adRepo.findById(userid).get();

            if (adinfo.getPassword().equals(password)) {

                session.setAttribute(
                        "admin",
                        adinfo.getUserid()
                );

                return "redirect:/admin/dashboard";

            } else {

                attributes.addFlashAttribute(
                        "msg",
                        "Invalid UserId or Password ⚠️"
                );
            }

            return "redirect:/adminlogin";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "User Does not Exists ❌"
            );

            return "redirect:/adminlogin";
        }
    }


    // =========================================================
    // FARMER REGISTRATION
    // =========================================================

    @GetMapping("/fregistration")
    public String ShowFormerRegistration(Model model) {

        model.addAttribute(
                "active4",
                "active"
        );

        FormersDto dto = new FormersDto();

        model.addAttribute(
                "dto",
                dto
        );

        return "formerregistration";
    }


    @PostMapping("/fregistration")
    public String FormerRegistration(
            @ModelAttribute FormersDto dto,
            RedirectAttributes attributes) {

        try {

            if (formersRepo.existsByEmail(dto.getEmail())) {

                attributes.addFlashAttribute(
                        "errormsg",
                        "User Already Exists ⚠️"
                );

                return "redirect:/fregistration";
            }

            Formers former = new Formers();

            former.setName(dto.getName());
            former.setEmail(dto.getEmail());
            former.setAadharno(dto.getAadharno());
            former.setContactno(dto.getContactno());
            former.setPassword(dto.getPassword());
            former.setAddress(dto.getAddress());

            Date dt = new Date();

            SimpleDateFormat df =
                    new SimpleDateFormat("dd/MM/yyyy");

            String regdate = df.format(dt);

            former.setRegdate(regdate);
            former.setStatus("Pending");

            formersRepo.save(former);

            attributes.addFlashAttribute(
                    "successmsg",
                    "Registration Successful ✅"
            );

            return "redirect:/fregistration";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "errormsg",
                    "Error : " + e.getMessage()
            );

            return "redirect:/fregistration";
        }
    }


    // =========================================================
    // FARMER LOGIN
    // =========================================================

    @GetMapping("/Flogin")
    public String ShowFormerLogin(Model model) {

        model.addAttribute(
                "dto",
                new FormersDto()
        );

        return "formerlogin";
    }


    @PostMapping("/Flogin")
    public String FormerLogin(
            @ModelAttribute FormersDto dto,
            RedirectAttributes attributes,
            HttpSession session) {

        try {

            Formers formers =
                    formersRepo.findFormerByEmail(
                            dto.getEmail()
                    );

            if (formers.getPassword()
                    .equals(dto.getPassword())) {

                if (formers.getStatus()
                        .equalsIgnoreCase("Verified")) {

                    /*
                     * Clear any previous account identity from this
                     * browser session before logging in as this farmer.
                     */
                    session.removeAttribute("loggedInFormer");
                    session.removeAttribute("former");
                    session.removeAttribute("loggedInMerchant");
                    session.removeAttribute("loggedInFPO");
                    session.removeAttribute("loggedInFPOId");

                    /*
                     * Keep both farmer session keys synchronized because
                     * existing farmer pages use both keys.
                     */
                    session.setAttribute(
                            "loggedInFormer",
                            formers
                    );

                    session.setAttribute(
                            "former",
                            formers
                    );

                    return "redirect:/Former/Dashboard";

                } else if (formers.getStatus()
                        .equalsIgnoreCase("Pending")) {

                    attributes.addFlashAttribute(
                            "error",
                            "Not Approved, Please wait for Admin Approval"
                    );

                    return "redirect:/Flogin";

                } else {

                    attributes.addFlashAttribute(
                            "error",
                            "Your Login is Disabled, Please Contact Administrator!"
                    );

                    return "redirect:/Flogin";
                }

            } else {

                attributes.addFlashAttribute(
                        "error",
                        "Invalid Password"
                );

                return "redirect:/Flogin";
            }

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "User does not Exists! ❌"
            );

            return "redirect:/Flogin";
        }
    }


    // =========================================================
    // MERCHANT REGISTRATION
    // =========================================================

    @GetMapping("/mregistration")
    public String ShowMerchantRegistration(Model model) {

        MerchantDto dto = new MerchantDto();

        model.addAttribute(
                "dto",
                dto
        );

        return "merchantregistration";
    }


    @PostMapping("/mregistration")
    public String MerchantRegistration(
            @ModelAttribute MerchantDto dto,
            RedirectAttributes attributes) {

        try {

            if (merchantRepo.existsByEmail(dto.getEmail())) {

                attributes.addFlashAttribute(
                        "errormsg",
                        "User Already Exists ⚠️"
                );

                return "redirect:/mregistration";
            }

            Merchant merchant = new Merchant();

            merchant.setName(dto.getName());
            merchant.setEmail(dto.getEmail());
            merchant.setContactno(dto.getContactno());
            merchant.setPassword(dto.getPassword());
            merchant.setAadharno(dto.getAadharno());
            merchant.setPancardno(dto.getPancardno());
            merchant.setAddress(dto.getAddress());
            merchant.setStatus("Pending");

            Date dt = new Date();

            SimpleDateFormat df =
                    new SimpleDateFormat("dd/MM/yyyy");

            String regdate = df.format(dt);

            merchant.setRegdate(regdate);

            merchantRepo.save(merchant);

            attributes.addFlashAttribute(
                    "successmsg",
                    "Registration Successfull ✅"
            );

            return "redirect:/mregistration";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "errormsg",
                    "Error : " + e.getMessage()
            );

            return "redirect:/mregistration";
        }
    }


    // =========================================================
    // MERCHANT LOGIN
    // =========================================================

    @GetMapping("/Mlogin")
    public String ShowMerchantLogin(Model model) {

        model.addAttribute(
                "dto",
                new MerchantDto()
        );

        return "merchantlogin";
    }


    @PostMapping("/Mlogin")
    public String MerchantLogin(
            @ModelAttribute MerchantDto dto,
            RedirectAttributes attributes,
            HttpSession session) {

        try {

            Merchant merchant =
                    merchantRepo.findByEmail(
                            dto.getEmail()
                    );

            if (merchant.getPassword()
                    .equals(dto.getPassword())) {

                if (merchant.getStatus()
                        .equalsIgnoreCase("Verified")) {

                    session.setAttribute(
                            "loggedInMerchant",
                            merchant
                    );

                    return "redirect:/Merchant/Dashboard";

                } else if (merchant.getStatus()
                        .equalsIgnoreCase("Pending")) {

                    attributes.addFlashAttribute(
                            "error",
                            "Not Approved, Please wait for Admin Approval"
                    );

                    return "redirect:/Mlogin";

                } else {

                    attributes.addFlashAttribute(
                            "error",
                            "Your Login is Disabled, Please Contact Administrator!"
                    );

                    return "redirect:/Mlogin";
                }

            } else {

                attributes.addFlashAttribute(
                        "error",
                        "Invalid Password"
                );

                return "redirect:/Mlogin";
            }

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "User does not Exists! ❌"
            );

            return "redirect:/Mlogin";
        }
    }


    // =========================================================
    // FPO REGISTRATION
    // =========================================================

    @GetMapping("/fporegistration")
    public String ShowFPORegistration(Model model) {

        model.addAttribute(
                "dto",
                new FPODto()
        );

        return "fporegistration";
    }


    @PostMapping("/fporegistration")
    public String FPORegistration(
            @ModelAttribute FPODto dto,
            RedirectAttributes attributes) {

        try {

            // -------------------------------------------------
            // CHECK EMAIL
            // -------------------------------------------------

            if (fpoRepo.findByEmail(dto.getEmail()).isPresent()) {

                attributes.addFlashAttribute(
                        "errormsg",
                        "FPO account with this email already exists ⚠️"
                );

                return "redirect:/fporegistration";
            }


            // -------------------------------------------------
            // CHECK REGISTRATION NUMBER
            // -------------------------------------------------

            boolean registrationExists =
                    fpoRepo.findAll()
                            .stream()
                            .anyMatch(fpo ->
                                    fpo.getRegistrationNumber()
                                            .equalsIgnoreCase(
                                                    dto.getRegistrationNumber()
                                            )
                            );

            if (registrationExists) {

                attributes.addFlashAttribute(
                        "errormsg",
                        "FPO Registration Number already exists ⚠️"
                );

                return "redirect:/fporegistration";
            }


            // -------------------------------------------------
            // CREATE FPO
            // -------------------------------------------------

            FPO fpo = new FPO();

            fpo.setName(dto.getName());

            fpo.setRegistrationNumber(
                    dto.getRegistrationNumber()
            );

            fpo.setContactPerson(
                    dto.getContactPerson()
            );

            fpo.setPhone(
                    dto.getPhone()
            );

            fpo.setEmail(
                    dto.getEmail()
            );

            fpo.setPassword(
                    dto.getPassword()
            );

            fpo.setAddress(
                    dto.getAddress()
            );

            fpo.setState(
                    dto.getState()
            );

            fpo.setDistrict(
                    dto.getDistrict()
            );

            fpo.setDescription(
                    dto.getDescription()
            );


            // New FPO accounts require admin approval
            fpo.setStatus("Pending");


            // -------------------------------------------------
            // REGISTRATION DATE
            // -------------------------------------------------

            Date dt = new Date();

            SimpleDateFormat df =
                    new SimpleDateFormat("dd/MM/yyyy");

            String regdate =
                    df.format(dt);

            fpo.setRegdate(regdate);


            // -------------------------------------------------
            // SAVE
            // -------------------------------------------------

            fpoRepo.save(fpo);


            attributes.addFlashAttribute(
                    "successmsg",
                    "FPO Registration Successful ✅ Please wait for Admin Approval."
            );

            return "redirect:/fporegistration";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "errormsg",
                    "Error : " + e.getMessage()
            );

            return "redirect:/fporegistration";
        }
    }


    // =========================================================
    // CONTACT US
    // =========================================================

    @GetMapping("/contactus")
    public String ShowContactUs(Model model) {

        EnquiryDto dto =
                new EnquiryDto();

        model.addAttribute(
                "dto",
                dto
        );

        model.addAttribute(
                "active5",
                "active"
        );

        return "contactus";
    }


    // =========================================================
    // CONTACT US FORM
    // =========================================================

    @PostMapping("/contactus")
    @ResponseBody
    public String ContactUs(
            @ModelAttribute EnquiryDto dto) {

        try {

            Enquiry enquiry =
                    new Enquiry();

            enquiry.setName(
                    dto.getName()
            );

            enquiry.setContactno(
                    dto.getContactno()
            );

            enquiry.setEmail(
                    dto.getEmail()
            );

            enquiry.setAddress(
                    dto.getAddress()
            );

            enquiry.setMessage(
                    dto.getMessage()
            );


            Date dt = new Date();

            SimpleDateFormat df =
                    new SimpleDateFormat("dd/MM/yyyy");

            String eqdate =
                    df.format(dt);

            enquiry.setEnquirydate(eqdate);

            eqRepo.save(enquiry);

            return "success";

        } catch (Exception e) {

            return "error -> : " + e.getMessage();
        }
    }
}