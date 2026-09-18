package com.softpro.ATH.Controller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.API.SendAutoEmail;
import com.softpro.ATH.Model.AdminInfo;
import com.softpro.ATH.Model.Category;
import com.softpro.ATH.Model.Enquiry;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Product;
import com.softpro.ATH.Model.MarketPriceReference;

import com.softpro.ATH.Repository.AdminInfoRepo;
import com.softpro.ATH.Repository.CategoryRepo;
import com.softpro.ATH.Repository.EnquiryRepo;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.MerchantRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.ProductRepo;
import com.softpro.ATH.Repository.MarketPriceReferenceRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    AdminInfoRepo adRepo;

    @Autowired
    EnquiryRepo eqRepo;

    @Autowired
    FormersRepo formersRepo;

    @Autowired
    MerchantRepo merchantRepo;

    @Autowired
    OrderRepo orderRepo;

    @Autowired
    ProductRepo productRepo;

    @Autowired
    CategoryRepo categoryRepo;

    @Autowired
    FPORepo fpoRepo;

    @Autowired
    private SendAutoEmail sendAutoEmail;

    @Autowired
    private MarketPriceReferenceRepo marketPriceReferenceRepo;


    /* =========================================================
       ADMIN DASHBOARD
       ========================================================= */

    @GetMapping("/dashboard")
    public String ShowDashboard(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).get();

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active1", "active");

            model.addAttribute("totalFarmers",
                    formersRepo.count());

            model.addAttribute("totalMerchants",
                    merchantRepo.count());

            model.addAttribute("totalFPOs",
                    fpoRepo.count());

            model.addAttribute("totalProducts",
                    productRepo.count());

            model.addAttribute(
                    "totalOrders",
                    orderRepo.countByOrderStatusAndDemoOrder(
                            "Confirmed", false)
                    +
                    orderRepo.countByOrderStatusAndDemoOrder(
                            "Pending", false)
                    +
                    orderRepo.countByOrderStatusAndDemoOrder(
                            "Delivered", false)
                    +
                    orderRepo.countByOrderStatusAndDemoOrder(
                            "Cancelled", false)
            );

            model.addAttribute(
                    "completedOrders",
                    orderRepo.countByOrderStatusAndDemoOrder(
                            "Delivered", false)
            );

            model.addAttribute(
                    "cancelledOrders",
                    orderRepo.countByOrderStatusAndDemoOrder(
                            "Cancelled", false)
            );

            model.addAttribute("newEnquiries",
                    eqRepo.count());

            return "admin/dashboard";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    /* =========================================================
       DEMO ACCESS MANAGEMENT
       ========================================================= */

    @GetMapping("/DemoAccess")
    public String ShowDemoAccess(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") == null) {

            attributes.addFlashAttribute(
                    "msg",
                    "Session Expired 👾"
            );

            return "redirect:/adminlogin";
        }

        AdminInfo admininfo =
                adRepo.findById(
                        session.getAttribute("admin").toString()
                ).orElse(null);

        if (admininfo == null) {
            session.invalidate();
            return "redirect:/adminlogin";
        }

        model.addAttribute("admininfo", admininfo);

        model.addAttribute(
                "merchants",
                merchantRepo.findAll()
        );

        model.addAttribute(
                "fpos",
                fpoRepo.findAll()
        );

        model.addAttribute(
                "farmers",
                formersRepo.findAll()
        );

        return "admin/demo-access";
    }


    /* =========================================================
       MERCHANT DEMO ACCESS
       ========================================================= */

    @PostMapping("/DemoAccess/Merchant")
    public String UpdateMerchantDemoAccess(
            @RequestParam("id") long id,
            @RequestParam("enabled") boolean enabled,
            HttpSession session,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") == null) {

            attributes.addFlashAttribute(
                    "msg",
                    "Session Expired 👾"
            );

            return "redirect:/adminlogin";
        }

        try {

            Merchant merchant =
                    merchantRepo.findById(id).orElseThrow();

            merchant.setDemoEnabled(enabled);

            merchantRepo.save(merchant);

            attributes.addFlashAttribute(
                    "msg",
                    enabled
                            ? "🧪 Demo access enabled for Merchant."
                            : "Demo access disabled for Merchant."
            );

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "Error updating Merchant Demo access: "
                            + e.getMessage()
            );
        }

        return "redirect:/admin/DemoAccess";
    }


    /* =========================================================
       FPO DEMO ACCESS
       ========================================================= */

    @PostMapping("/DemoAccess/FPO")
    public String UpdateFPODemoAccess(
            @RequestParam("id") long id,
            @RequestParam("enabled") boolean enabled,
            HttpSession session,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") == null) {

            attributes.addFlashAttribute(
                    "msg",
                    "Session Expired 👾"
            );

            return "redirect:/adminlogin";
        }

        try {

            FPO fpo =
                    fpoRepo.findById(id).orElseThrow();

            fpo.setDemoEnabled(enabled);

            fpoRepo.save(fpo);

            attributes.addFlashAttribute(
                    "msg",
                    enabled
                            ? "🧪 Demo access enabled for FPO."
                            : "Demo access disabled for FPO."
            );

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "Error updating FPO Demo access: "
                            + e.getMessage()
            );
        }

        return "redirect:/admin/DemoAccess";
    }


    /* =========================================================
       FARMER DEMO ACCESS
       ========================================================= */

    @PostMapping("/DemoAccess/Farmer")
    public String UpdateFarmerDemoAccess(
            @RequestParam("id") long id,
            @RequestParam("enabled") boolean enabled,
            HttpSession session,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") == null) {

            attributes.addFlashAttribute(
                    "msg",
                    "Session Expired 👾"
            );

            return "redirect:/adminlogin";
        }

        try {

            Formers farmer =
                    formersRepo.findById(id).orElseThrow();

            farmer.setDemoEnabled(enabled);

            formersRepo.save(farmer);

            attributes.addFlashAttribute(
                    "msg",
                    enabled
                            ? "🧪 Demo access enabled for Independent Farmer."
                            : "Demo access disabled for Independent Farmer."
            );

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "Error updating Farmer Demo access: "
                            + e.getMessage()
            );
        }

        return "redirect:/admin/DemoAccess";
    }


    /* =========================================================
       EDIT PROFILE
       ========================================================= */

    @GetMapping("/EditProfile")
    public String ShowEditProfile(
            Model model,
            HttpSession session) {

        if (session.getAttribute("admin") == null)
            return "redirect:/adminlogin";

        AdminInfo admininfo =
                adRepo.findById(
                        session.getAttribute("admin").toString()
                ).orElse(null);

        if (admininfo == null) {
            session.invalidate();
            return "redirect:/adminlogin";
        }

        model.addAttribute("admininfo", admininfo);

        return "admin/EditProfile";
    }


    /* =========================================================
       UPDATE PROFILE
       ========================================================= */

    @PostMapping("/UpdateProfile")
    public String UpdateProfilePic(
            @RequestParam("file")
            org.springframework.web.multipart.MultipartFile file,
            RedirectAttributes attributes,
            HttpSession session) {

        try {

            String storageFileName =
                    System.currentTimeMillis()
                    + "_"
                    + file.getOriginalFilename();

            String uploadDir =
                    "public/ProfilePic/";

            Path uploadPath =
                    Paths.get(uploadDir);

            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElseThrow();

            String oldProfilePic =
                    admininfo.getProfilepic();

            if (oldProfilePic != null
                    && !oldProfilePic.isEmpty()) {

                Path oldFilePath =
                        Paths.get(
                                uploadDir
                                + oldProfilePic
                        );

                if (Files.exists(oldFilePath))
                    Files.delete(oldFilePath);
            }

            try (InputStream inputStream =
                         file.getInputStream()) {

                Files.copy(
                        inputStream,
                        Paths.get(
                                uploadDir
                                + storageFileName
                        ),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            admininfo.setProfilepic(
                    storageFileName
            );

            adRepo.save(admininfo);

            return "redirect:/admin/EditProfile";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "Error : " + e.getMessage()
            );

            return "redirect:/admin/EditProfile";
        }
    }


    /* =========================================================
       FARMER MANAGEMENT
       ========================================================= */

    @GetMapping("/newformer")
    public String NewFormer(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active3", "active");

            List<Formers> FList =
                    formersRepo.findAllByStatus("Pending");

            model.addAttribute(
                    "FList",
                    FList.reversed()
            );

            return "admin/newformer";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/verifiedformer")
    public String VerifiedFormer(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active3", "active");

            List<Formers> FList =
                    formersRepo.findAllByStatus("Verified");

            model.addAttribute(
                    "FList",
                    FList.reversed()
            );

            return "admin/verifiedformer";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/disableformer")
    public String DisabledFormer(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active3", "active");

            List<Formers> FList =
                    formersRepo.findAllByStatus("Disabled");

            model.addAttribute(
                    "FList",
                    FList.reversed()
            );

            return "admin/disableformer";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/formerstatus")
    public String FormerStatusUpdate(
            @RequestParam("id") long id) {

        try {

            Formers former =
                    formersRepo.findById(id).orElseThrow();

            List<Product> products =
                    productRepo.findByFarmer(former);

            if (former.getStatus().equals("Pending")) {

                former.setStatus("Verified");

                formersRepo.save(former);

                sendAutoEmail.sendApprovalEmail(former);

                return "redirect:/admin/newformer";

            } else if (former.getStatus().equals("Verified")) {

                former.setStatus("Disabled");

                formersRepo.save(former);

                for (Product product : products) {

                    if (product.getQuantity() > 0)
                        product.setStatus("OutOfStock");
                }

                productRepo.saveAll(products);

                return "redirect:/admin/verifiedformer";

            } else {

                former.setStatus("Verified");

                formersRepo.save(former);

                for (Product product : products) {

                    if (product.getQuantity() > 0)
                        product.setStatus("Available");
                }

                productRepo.saveAll(products);

                return "redirect:/admin/disableformer";
            }

        } catch (Exception e) {

            System.err.println(
                    "Error : " + e.getMessage()
            );

            return "redirect:/admin/newformer";
        }
    }


    /* =========================================================
       MERCHANT MANAGEMENT
       ========================================================= */

    @GetMapping("/newmerchant")
    public String NewMerchant(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active2", "active");

            List<Merchant> MList =
                    merchantRepo.findAllByStatus("Pending");

            model.addAttribute(
                    "MList",
                    MList.reversed()
            );

            return "admin/newmerchant";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/verifiedmerchant")
    public String VerifiedMerchant(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active2", "active");

            List<Merchant> MList =
                    merchantRepo.findAllByStatus("Verified");

            model.addAttribute(
                    "MList",
                    MList.reversed()
            );

            return "admin/verifiedmerchant";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/disablemerchant")
    public String DisabledMerchant(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active2", "active");

            List<Merchant> MList =
                    merchantRepo.findAllByStatus("Disabled");

            model.addAttribute(
                    "MList",
                    MList.reversed()
            );

            return "admin/disablemerchant";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/merchantstatus")
    public String MerchantStatusUpdate(
            @RequestParam("id") long id) {

        try {

            Merchant merchant =
                    merchantRepo.findById(id).orElseThrow();

            if (merchant.getStatus().equals("Pending")) {

                merchant.setStatus("Verified");

                merchantRepo.save(merchant);

                sendAutoEmail.sendApprovalEmail(merchant);

                return "redirect:/admin/newmerchant";

            } else if (merchant.getStatus().equals("Verified")) {

                merchant.setStatus("Disabled");

                merchantRepo.save(merchant);

                return "redirect:/admin/verifiedmerchant";

            } else {

                merchant.setStatus("Verified");

                merchantRepo.save(merchant);

                return "redirect:/admin/disablemerchant";
            }

        } catch (Exception e) {

            System.err.println(
                    "Error : " + e.getMessage()
            );

            return "redirect:/admin/newmerchant";
        }
    }


    /* =========================================================
       FPO MANAGEMENT
       ========================================================= */

    @GetMapping("/newfpo")
    public String NewFPO(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active7", "active");

            List<FPO> fpoList =
                    fpoRepo.findAllByStatus("Pending");

            model.addAttribute(
                    "FPOList",
                    fpoList.reversed()
            );

            return "admin/newfpo";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/verifiedfpo")
    public String VerifiedFPO(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active7", "active");

            List<FPO> fpoList =
                    fpoRepo.findAllByStatus("Verified");

            model.addAttribute(
                    "FPOList",
                    fpoList.reversed()
            );

            return "admin/verifiedfpo";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/disablefpo")
    public String DisabledFPO(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute("admininfo", admininfo);
            model.addAttribute("active7", "active");

            List<FPO> fpoList =
                    fpoRepo.findAllByStatus("Disabled");

            model.addAttribute(
                    "FPOList",
                    fpoList.reversed()
            );

            return "admin/disablefpo";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/fpostatus")
    public String FPOStatusUpdate(
            @RequestParam("id") long id) {

        try {

            FPO fpo =
                    fpoRepo.findById(id).orElseThrow();

            if ("Pending".equals(fpo.getStatus())) {

                fpo.setStatus("Verified");

                fpoRepo.save(fpo);

                return "redirect:/admin/newfpo";

            } else if ("Verified".equals(fpo.getStatus())) {

                fpo.setStatus("Disabled");

                fpoRepo.save(fpo);

                return "redirect:/admin/verifiedfpo";

            } else {

                fpo.setStatus("Verified");

                fpoRepo.save(fpo);

                return "redirect:/admin/disablefpo";
            }

        } catch (Exception e) {

            System.err.println(
                    "FPO status error: "
                            + e.getMessage()
            );

            return "redirect:/admin/newfpo";
        }
    }


    /* =========================================================
       VIEW ORDERS
       ========================================================= */

    @GetMapping("/ViewOrder")
    public String ShowViewOrders(
            Model model,
            HttpSession session) {

        if (session.getAttribute("admin") == null)
            return "redirect:/adminlogin";

        AdminInfo admininfo =
                adRepo.findById(
                        session.getAttribute("admin").toString()
                ).orElse(null);

        if (admininfo == null)
            return "redirect:/adminlogin";

        model.addAttribute(
                "admininfo",
                admininfo
        );

        List<Order> orderList =
                orderRepo.findAll();

        model.addAttribute(
                "orderList",
                orderList.reversed()
        );

        return "admin/vieworders";
    }


    /* =========================================================
       CATEGORY
       ========================================================= */

    @GetMapping("/AddCategory")
    public String ShowAddCategory(
            Model model,
            HttpSession session) {

        if (session.getAttribute("admin") == null)
            return "redirect:/adminlogin";

        AdminInfo admininfo =
                adRepo.findById(
                        session.getAttribute("admin").toString()
                ).orElse(null);

        if (admininfo == null)
            return "redirect:/adminlogin";

        model.addAttribute(
                "admininfo",
                admininfo
        );

        model.addAttribute(
                "active5",
                "active"
        );

        return "admin/addcategory";
    }


    @PostMapping("/AddCategory")
    public String AddCategory(
            @RequestParam("category") String cate,
            RedirectAttributes attributes) {

        try {

            Category category =
                    new Category();

            if (categoryRepo.existsByCategory(cate)) {

                attributes.addFlashAttribute(
                        "msg",
                        "This category Already Exists!"
                );

                return "redirect:/admin/AddCategory";
            }

            category.setCategory(cate);

            categoryRepo.save(category);

            attributes.addFlashAttribute(
                    "msg",
                    "Category Successfully Added"
            );

            return "redirect:/admin/AddCategory";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "Error : " + e.getMessage()
            );

            return "redirect:/admin/AddCategory";
        }
    }


    /* =========================================================
       ENQUIRY
       ========================================================= */

    @GetMapping("/enquiry")
    public String ShowEnquiry(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute(
                    "admininfo",
                    admininfo
            );

            model.addAttribute(
                    "active6",
                    "active"
            );

            List<Enquiry> enquiryList =
                    eqRepo.findAll();

            model.addAttribute(
                    "enquiryList",
                    enquiryList
            );

            return "admin/enquiry";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @GetMapping("/enquirydelete")
    public String DeleteEnquiry(
            @RequestParam("id") long id,
            RedirectAttributes attributes) {

        try {

            Enquiry enquiry =
                    eqRepo.findById(id).orElseThrow();

            eqRepo.delete(enquiry);

        } catch (Exception e) {

            System.err.println(
                    "Error : " + e.getMessage()
            );
        }

        return "redirect:/admin/enquiry";
    }


    /* =========================================================
       CHANGE PASSWORD
       ========================================================= */

    @GetMapping("/ChangePassword")
    public String ShowChangePassword(
            HttpSession session,
            RedirectAttributes attributes,
            Model model) {

        if (session.getAttribute("admin") != null) {

            AdminInfo admininfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElse(null);

            if (admininfo == null)
                return "redirect:/adminlogin";

            model.addAttribute(
                    "admininfo",
                    admininfo
            );

            return "admin/changepassword";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    @PostMapping("/ChangePassword")
    public String ChangePassword(
            HttpServletRequest request,
            RedirectAttributes attributes,
            HttpSession session) {

        try {

            AdminInfo adInfo =
                    adRepo.findById(
                            session.getAttribute("admin").toString()
                    ).orElseThrow();

            String newPassword =
                    request.getParameter("newpass");

            String confirmPassword =
                    request.getParameter("confirmpass");

            String oldPassword =
                    request.getParameter("oldpass");

            if (!newPassword.equals(confirmPassword)) {

                attributes.addFlashAttribute(
                        "error",
                        "New password and Confirm Password do not match."
                );

                return "redirect:/admin/ChangePassword";
            }

            if (!adInfo.getPassword().equals(oldPassword)) {

                attributes.addFlashAttribute(
                        "error",
                        "Invalid Old Password."
                );

                return "redirect:/admin/ChangePassword";
            }

            adInfo.setPassword(confirmPassword);

            adRepo.save(adInfo);

            session.invalidate();

            attributes.addFlashAttribute(
                    "msg",
                    "Password Successfully Changed"
            );

            return "redirect:/adminlogin";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Error :" + e.getMessage()
            );

            return "redirect:/admin/ChangePassword";
        }
    }


    /* =========================================================
       LOGOUT
       ========================================================= */

    @GetMapping("/logout")
    public String Logout(
            HttpSession session,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") != null) {

            session.removeAttribute("admin");

            attributes.addFlashAttribute(
                    "msg",
                    "Successfully Logout ✅"
            );

            return "redirect:/adminlogin";
        }

        attributes.addFlashAttribute(
                "msg",
                "Session Expired 👾"
        );

        return "redirect:/adminlogin";
    }


    /* =========================================================
       MARKET PRICE REFERENCES
       ========================================================= */

    @GetMapping("/market-prices")
    public String marketPrices(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Admin session expired."
            );

            return "redirect:/adminlogin";
        }

        AdminInfo admininfo =
                adRepo.findById(
                        session.getAttribute("admin").toString()
                ).orElse(null);

        if (admininfo == null) {

            session.invalidate();

            attributes.addFlashAttribute(
                    "error",
                    "Admin session expired."
            );

            return "redirect:/adminlogin";
        }

        model.addAttribute(
                "admininfo",
                admininfo
        );

        model.addAttribute(
                "references",
                marketPriceReferenceRepo.findAll()
        );

        model.addAttribute(
                "activeMarketPrices",
                "active"
        );

        return "admin/market-prices";
    }


    @PostMapping("/market-prices")
    public String saveMarketPrice(
            HttpSession session,
            @RequestParam String productName,
            @RequestParam(required = false) String category,
            @RequestParam BigDecimal referencePrice,
            @RequestParam(defaultValue = "10")
            double lowerPercentage,
            @RequestParam(defaultValue = "10")
            double upperPercentage,
            RedirectAttributes attributes) {

        if (session.getAttribute("admin") == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Admin session expired."
            );

            return "redirect:/adminlogin";
        }

        try {

            if (productName == null
                    || productName.isBlank()
                    || referencePrice == null
                    || referencePrice.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                throw new IllegalArgumentException(
                        "Product name and a positive reference price are required."
                );
            }

            MarketPriceReference ref =
                    new MarketPriceReference();

            ref.setProductName(
                    productName.trim()
            );

            ref.setCategory(
                    category == null
                            ? null
                            : category.trim()
            );

            ref.setReferencePrice(
                    referencePrice.setScale(
                            2,
                            java.math.RoundingMode.HALF_UP
                    )
            );

            ref.setLowerPercentage(
                    lowerPercentage
            );

            ref.setUpperPercentage(
                    upperPercentage
            );

            ref.setUpdatedAt(
                    LocalDateTime.now()
            );

            marketPriceReferenceRepo.save(ref);

            attributes.addFlashAttribute(
                    "success",
                    "Market price reference saved."
            );

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/market-prices";
    }
}