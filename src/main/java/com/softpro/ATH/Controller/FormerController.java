
package com.softpro.ATH.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.API.PaymentService;
import com.softpro.ATH.API.SendAutoEmail;
import com.softpro.ATH.Dto.ProductDto;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.FPOProductContribution;
import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Payment;
import com.softpro.ATH.Model.Product;
import com.softpro.ATH.Repository.CategoryRepo;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FPOProductContributionRepo;
import com.softpro.ATH.Repository.FPOShipmentRepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.PaymentRepo;
import com.softpro.ATH.Repository.ProductRepo;
import com.softpro.ATH.Service.DemoAccessService;
import com.softpro.ATH.Service.FairPriceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@ControllerAdvice
@RequestMapping("/Former")
public class FormerController {

    @Autowired
    private HttpSession session;

    @Autowired
    private FormersRepo formersRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private SendAutoEmail sendAutoEmail;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FPORepo fpoRepo;

    @Autowired
    private FPOProductContributionRepo contributionRepo;

    @Autowired
    private FPOShipmentRepo shipmentRepo;

    @Autowired
    private DemoAccessService demoAccessService;

    @Autowired
    private FairPriceService fairPriceService;


    // =========================================================
    // DASHBOARD
    // =========================================================

    @GetMapping({"/Dashboard", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {

        Formers farmer = getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        model.addAttribute("active1", "active");

        List<Order> recentOrders =
                orderRepo.findTop5ByFarmerAndDemoOrderOrderByOrderDateDesc(
                        farmer,
                        false
                );

        BigDecimal totalRevenue =
                orderRepo.getTotalRevenueByFarmerId(farmer.getId());

        BigDecimal monthlyRevenue =
                orderRepo.getCurrentMonthRevenue(farmer.getId());

        BigDecimal inStockRevenue =
                productRepo.calculateInStockRevenue(farmer.getId());

        String topProduct =
                orderRepo.getMostOrderedProduct(farmer.getId());

        model.addAttribute("former", farmer);

        model.addAttribute(
                "totalProducts",
                productRepo.countByFarmer(farmer)
        );

        model.addAttribute(
                "totalOrders",
                orderRepo.countByFarmerAndDemoOrder(farmer, false)
        );

        model.addAttribute(
                "completedOrders",
                orderRepo.countByFarmerAndDemoOrderAndOrderStatus(
                        farmer,
                        false,
                        "Delivered"
                )
        );

        model.addAttribute(
                "cancelledOrders",
                orderRepo.countByFarmerAndDemoOrderAndOrderStatus(
                        farmer,
                        false,
                        "Cancelled"
                )
        );

        model.addAttribute("recentOrders", recentOrders);

        model.addAttribute(
                "totalRevenue",
                totalRevenue != null
                        ? totalRevenue
                        : BigDecimal.ZERO
        );

        model.addAttribute(
                "monthlyRevenue",
                monthlyRevenue != null
                        ? monthlyRevenue
                        : BigDecimal.ZERO
        );

        model.addAttribute(
                "inStockRevenue",
                inStockRevenue != null
                        ? inStockRevenue
                        : BigDecimal.ZERO
        );

        model.addAttribute(
                "topProduct",
                topProduct != null
                        ? topProduct
                        : "N/A"
        );

        model.addAttribute(
                "memberFPOs",
                fpoRepo.findByMembersContaining(farmer)
        );

        return "Former/dashboard";
    }


    // =========================================================
    // EDIT PROFILE
    // =========================================================

    @GetMapping("/EditProfile")
    public String showEditProfile(
            Model model,
            HttpSession session) {

        Formers farmer = getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        model.addAttribute("former", farmer);

        return "Former/EditProfile";
    }


    @PostMapping("/UpdateProfile")
    public String updateProfile(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute("former") Formers oldFormer,
            RedirectAttributes attributes,
            HttpSession session) {

        try {

            Formers farmer = getLoggedInFormer(session);

            if (farmer == null) {
                return "redirect:/Flogin";
            }

            if (file != null && !file.isEmpty()) {

                String storageFileName =
                        System.currentTimeMillis()
                                + "_"
                                + file.getOriginalFilename();

                String uploadDir = "public/ProfilePic/";

                Path uploadPath = Paths.get(uploadDir);

                Files.createDirectories(uploadPath);

                String oldProfilePic =
                        farmer.getProfilepic();

                if (oldProfilePic != null
                        && !oldProfilePic.isBlank()) {

                    Files.deleteIfExists(
                            uploadPath.resolve(oldProfilePic)
                    );
                }

                try (InputStream inputStream =
                             file.getInputStream()) {

                    Files.copy(
                            inputStream,
                            uploadPath.resolve(storageFileName),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                farmer.setProfilepic(storageFileName);
            }

            farmer.setName(oldFormer.getName());
            farmer.setContactno(oldFormer.getContactno());
            farmer.setAadharno(oldFormer.getAadharno());
            farmer.setAddress(oldFormer.getAddress());

            formersRepo.save(farmer);

            session.setAttribute(
                    "loggedInFormer",
                    farmer
            );

            attributes.addFlashAttribute(
                    "msg",
                    "Profile updated successfully."
            );

            return "redirect:/Former/EditProfile";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "Error : " + e.getMessage()
            );

            return "redirect:/Former/EditProfile";
        }
    }


    // =========================================================
    // ADD PRODUCT - GET
    // =========================================================

    @GetMapping("/AddProduct")
    public String showAddProduct(
            Model model,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        model.addAttribute(
                "productDto",
                new ProductDto()
        );

        model.addAttribute(
                "categories",
                categoryRepo.findAll()
        );

        model.addAttribute(
                "memberFPOs",
                fpoRepo.findByMembersContaining(farmer)
        );

        model.addAttribute(
                "active2",
                "active"
        );

        return "Former/addproduct";
    }


    // =========================================================
    // ADD PRODUCT - POST
    // =========================================================

    @PostMapping("/AddProduct")
    public String addProduct(
            @ModelAttribute ProductDto dto,
            RedirectAttributes attributes,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        try {

            Product product = new Product();

            // -------------------------------------------------
            // BASIC PRODUCT DATA
            // -------------------------------------------------

            product.setProductName(
                    dto.getProductName()
            );

            product.setCategory(
                    dto.getCategory()
            );

            product.setQuantity(
                    dto.getQuantity()
            );

            // -------------------------------------------------
            // PRICE VALIDATION
            // -------------------------------------------------

            if (dto.getPricePerUnit() == null
                    || dto.getPricePerUnit()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Price per unit must be greater than 0."
                );
            }

            product.setPricePerUnit(
                    dto.getPricePerUnit()
            );

            // -------------------------------------------------
            // FAIR PRICE CHECK
            // -------------------------------------------------

            com.softpro.ATH.Dto.FairPriceResponse fairCheck =
                    fairPriceService.checkPrice(
                            dto.getProductName(),
                            dto.getCategory(),
                            dto.getPricePerUnit()
                    );

            if ("ABOVE_MARKET".equals(
                    fairCheck.getStatus())) {

                throw new IllegalArgumentException(
                        fairCheck.getMessage()
                                + " Reference: ₹"
                                + fairCheck.getReferencePrice()
                                + ", fair upper limit: ₹"
                                + fairCheck.getUpperFairPrice()
                                + "."
                );
            }

            // -------------------------------------------------
            // NORMAL ORDER QUANTITY RULES
            // -------------------------------------------------

            if (dto.getMinimumOrderQuantity() < 1) {

                throw new IllegalArgumentException(
                        "Minimum order quantity must be at least 1."
                );
            }

            if (dto.getMaxOrderQuantity() < 0) {

                throw new IllegalArgumentException(
                        "Maximum order quantity cannot be negative."
                );
            }

            if (dto.getMaxOrderQuantity() > 0
                    && dto.getMaxOrderQuantity()
                    < dto.getMinimumOrderQuantity()) {

                throw new IllegalArgumentException(
                        "Maximum order quantity cannot be less than minimum order quantity."
                );
            }

            product.setMinimumOrderQuantity(
                    dto.getMinimumOrderQuantity()
            );

            product.setMaxOrderQuantity(
                    dto.getMaxOrderQuantity()
            );

            // -------------------------------------------------
            // BULK ORDER RULES
            // -------------------------------------------------

            if (dto.getBulkMinimumQuantity() < 1) {

                throw new IllegalArgumentException(
                        "Bulk minimum quantity must be at least 1."
                );
            }

            if (dto.getMaxBulkQuantity() < 0) {

                throw new IllegalArgumentException(
                        "Maximum bulk quantity cannot be negative."
                );
            }

            if (dto.getMaxBulkQuantity() > 0
                    && dto.getMaxBulkQuantity()
                    < dto.getBulkMinimumQuantity()) {

                throw new IllegalArgumentException(
                        "Maximum bulk quantity cannot be less than bulk minimum quantity."
                );
            }

            product.setBulkMinimumQuantity(
                    dto.getBulkMinimumQuantity()
            );

            product.setMaxBulkQuantity(
                    dto.getMaxBulkQuantity()
            );

            product.setBulkPricePerUnit(
                    dto.getBulkPricePerUnit()
            );

            // -------------------------------------------------
            // SELLING MODE
            // -------------------------------------------------

            String sellingMode =
                    dto.getSellingMode();

            if (sellingMode == null
                    || sellingMode.isBlank()) {

                sellingMode = "INDEPENDENT";
            }

            sellingMode =
                    sellingMode.trim().toUpperCase();

            if (!sellingMode.equals("INDEPENDENT")
                    && !sellingMode.equals("FPO")) {

                throw new IllegalArgumentException(
                        "Invalid selling mode."
                );
            }

            // -------------------------------------------------
            // FPO VALIDATION
            // -------------------------------------------------

            if (sellingMode.equals("FPO")) {

                if (dto.getFpoId() == null) {

                    throw new IllegalArgumentException(
                            "Select an FPO for FPO contribution."
                    );
                }

                FPO selectedFpo =
                        fpoRepo.findById(
                                dto.getFpoId()
                        ).orElse(null);

                if (selectedFpo == null) {

                    throw new IllegalArgumentException(
                            "Selected FPO was not found."
                    );
                }

                boolean memberOfSelectedFpo =
                        fpoRepo.findByMembersContaining(farmer)
                                .stream()
                                .anyMatch(
                                        memberFpo ->
                                                memberFpo.getId()
                                                        == selectedFpo.getId()
                                );

                // IMPORTANT:
                // getId() returns primitive long,
                // therefore use == and NOT .equals()

                if (!memberOfSelectedFpo) {

                    throw new IllegalArgumentException(
                            "You can contribute to an FPO only if you are a member of that FPO."
                    );
                }

                product.setFpo(selectedFpo);

            } else {

                product.setFpo(null);
                product.setFpoProduct(null);
            }

            product.setSellingMode(
                    sellingMode
            );

            // -------------------------------------------------
            // STATUS
            // -------------------------------------------------

            product.setStatus(
                    dto.getQuantity() > 0
                            ? "Available"
                            : "OutOfStock"
            );

            product.setFarmer(farmer);

            // -------------------------------------------------
            // IMAGE
            // -------------------------------------------------

            MultipartFile file =
                    dto.getImage();

            if (file != null
                    && !file.isEmpty()) {

                String storageFileName =
                        UUID.randomUUID()
                                + "_"
                                + file.getOriginalFilename();

                String uploadDir =
                        "Public/ProductImage/";

                Path uploadPath =
                        Paths.get(uploadDir);

                Files.createDirectories(
                        uploadPath
                );

                try (InputStream inputStream =
                             file.getInputStream()) {

                    Files.copy(
                            inputStream,
                            uploadPath.resolve(
                                    storageFileName
                            ),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                product.setImage(
                        storageFileName
                );
            }

            // -------------------------------------------------
            // SAVE PRODUCT
            // -------------------------------------------------

            productRepo.save(product);

            // -------------------------------------------------
            // FPO EMAIL
            // -------------------------------------------------

            if ("FPO".equalsIgnoreCase(
                    product.getSellingMode())
                    && product.getFpo() != null) {

                sendAutoEmail
                        .SendFPOContributionCommittedEmail(
                                product.getFpo(),
                                farmer,
                                product
                        );
            }

            attributes.addFlashAttribute(
                    "msg",
                    "Product Added Successfully"
            );

            attributes.addFlashAttribute(
                    "success",
                    "Product added successfully!"
            );

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Unable to add product: "
                            + e.getMessage()
            );
        }

        return "redirect:/Former/AddProduct";
    }


    // =========================================================
    // MANAGE PRODUCTS
    // =========================================================

    @GetMapping("/ManageProduct")
    public String manageProducts(
            Model model,
            @RequestParam(
                    name = "status",
                    required = false
            ) String status,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        List<Product> products;

        if (status != null
                && !status.isBlank()) {

            products =
                    productRepo.findByFarmerAndStatus(
                            farmer,
                            status
                    );

        } else {

            products =
                    productRepo.findByFarmer(
                            farmer
                    );
        }

        model.addAttribute(
                "former",
                farmer
        );

        model.addAttribute(
                "productList",
                products
        );

        model.addAttribute(
                "products",
                products
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        model.addAttribute(
                "active3",
                "active"
        );

        return "Former/manageproduct";
    }


    // =========================================================
    // FPO CONTRIBUTION TRANSPARENCY
    // =========================================================

    @GetMapping("/FPOContributions")
    public String fpoContributions(
            Model model,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        List<FPOProductContribution> contributions =
                contributionRepo.findByFarmer(farmer);

        int totalContributed =
                contributions.stream()
                        .mapToInt(
                                FPOProductContribution
                                        ::getQuantityContributed
                        )
                        .sum();

        int totalAvailable =
                contributions.stream()
                        .mapToInt(
                                FPOProductContribution
                                        ::getQuantityAvailable
                        )
                        .sum();

        int totalSold =
                totalContributed - totalAvailable;

        BigDecimal totalValue =
                contributions.stream()
                        .map(c -> {

                            BigDecimal farmerRate =
                                    c.getSourceProduct() != null
                                            && c.getSourceProduct()
                                            .getPricePerUnit() != null

                                            ? c.getSourceProduct()
                                            .getPricePerUnit()

                                            : BigDecimal.ZERO;

                            return farmerRate.multiply(
                                    BigDecimal.valueOf(
                                            c.getQuantityContributed()
                                    )
                            );
                        })
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal soldValue =
                contributions.stream()
                        .map(c -> {

                            BigDecimal farmerRate =
                                    c.getSourceProduct() != null
                                            && c.getSourceProduct()
                                            .getPricePerUnit() != null

                                            ? c.getSourceProduct()
                                            .getPricePerUnit()

                                            : BigDecimal.ZERO;

                            int soldQuantity =
                                    c.getQuantityContributed()
                                            - c.getQuantityAvailable();

                            return farmerRate.multiply(
                                    BigDecimal.valueOf(
                                            soldQuantity
                                    )
                            );
                        })
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        model.addAttribute(
                "former",
                farmer
        );

        model.addAttribute(
                "contributions",
                contributions
        );

        model.addAttribute(
                "totalContributed",
                totalContributed
        );

        model.addAttribute(
                "totalAvailable",
                totalAvailable
        );

        model.addAttribute(
                "totalSold",
                totalSold
        );

        model.addAttribute(
                "totalValue",
                totalValue
        );

        model.addAttribute(
                "soldValue",
                soldValue
        );

        model.addAttribute(
                "active5",
                "active"
        );

        return "Former/fpocontributions";
    }


    // =========================================================
    // EDIT PRODUCT - GET
    // =========================================================

    @GetMapping("/EditProduct")
    public String showEditProduct(
            @RequestParam("id") long id,
            Model model,
            HttpSession session,
            RedirectAttributes attributes) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        Optional<Product> optionalProduct =
                productRepo.findById(id);

        if (optionalProduct.isEmpty()) {

            attributes.addFlashAttribute(
                    "error",
                    "Product not found."
            );

            return "redirect:/Former/ManageProduct";
        }

        Product product =
                optionalProduct.get();

        if (!isOwner(product, farmer)) {

            attributes.addFlashAttribute(
                    "error",
                    "You are not authorized to edit this product."
            );

            return "redirect:/Former/ManageProduct";
        }

        model.addAttribute(
                "product",
                product
        );

        model.addAttribute(
                "categories",
                categoryRepo.findAll()
        );

        model.addAttribute(
                "memberFPOs",
                fpoRepo.findByMembersContaining(farmer)
        );

        return "Former/EditProduct";
    }


    // =========================================================
    // EDIT PRODUCT - PATH COMPATIBILITY
    // =========================================================

    @GetMapping("/EditProduct/{id}")
    public String showEditProductByPath(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            Model model,
            HttpSession session,
            RedirectAttributes attributes) {

        return showEditProduct(
                id,
                model,
                session,
                attributes
        );
    }


    // =========================================================
    // EDIT PRODUCT - POST
    // =========================================================

    @PostMapping("/EditProduct")
    public String editProduct(
            @ModelAttribute ProductDto dto,
            @RequestParam(
                    value = "file",
                    required = false
            ) MultipartFile file,
            RedirectAttributes attributes,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        try {

            Product product =
                    productRepo.findById(
                            dto.getId()
                    ).orElse(null);

            if (product == null) {

                attributes.addFlashAttribute(
                        "error",
                        "Product not found."
                );

                return "redirect:/Former/ManageProduct";
            }

            if (!isOwner(product, farmer)) {

                attributes.addFlashAttribute(
                        "error",
                        "You are not authorized to edit this product."
                );

                return "redirect:/Former/ManageProduct";
            }

            // -------------------------------------------------
            // BASIC DATA
            // -------------------------------------------------

            product.setProductName(
                    dto.getProductName()
            );

            product.setCategory(
                    dto.getCategory()
            );

            product.setQuantity(
                    dto.getQuantity()
            );

            // -------------------------------------------------
            // PRICE
            // -------------------------------------------------

            if (dto.getPricePerUnit() == null
                    || dto.getPricePerUnit()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Price per unit must be greater than 0."
                );
            }

            product.setPricePerUnit(
                    dto.getPricePerUnit()
            );

            com.softpro.ATH.Dto.FairPriceResponse fairCheck =
                    fairPriceService.checkPrice(
                            dto.getProductName(),
                            dto.getCategory(),
                            dto.getPricePerUnit()
                    );

            if ("ABOVE_MARKET".equals(
                    fairCheck.getStatus())) {

                throw new IllegalArgumentException(
                        fairCheck.getMessage()
                                + " Reference: ₹"
                                + fairCheck.getReferencePrice()
                                + ", fair upper limit: ₹"
                                + fairCheck.getUpperFairPrice()
                                + "."
                );
            }

            // -------------------------------------------------
            // NORMAL ORDER RULES
            // -------------------------------------------------

            if (dto.getMinimumOrderQuantity() < 1) {

                throw new IllegalArgumentException(
                        "Minimum order quantity must be at least 1."
                );
            }

            if (dto.getMaxOrderQuantity() < 0) {

                throw new IllegalArgumentException(
                        "Maximum order quantity cannot be negative."
                );
            }

            if (dto.getMaxOrderQuantity() > 0
                    && dto.getMaxOrderQuantity()
                    < dto.getMinimumOrderQuantity()) {

                throw new IllegalArgumentException(
                        "Maximum order quantity cannot be less than minimum order quantity."
                );
            }

            product.setMinimumOrderQuantity(
                    dto.getMinimumOrderQuantity()
            );

            product.setMaxOrderQuantity(
                    dto.getMaxOrderQuantity()
            );

            // -------------------------------------------------
            // BULK RULES
            // -------------------------------------------------

            if (dto.getBulkMinimumQuantity() < 1) {

                throw new IllegalArgumentException(
                        "Bulk minimum quantity must be at least 1."
                );
            }

            if (dto.getMaxBulkQuantity() < 0) {

                throw new IllegalArgumentException(
                        "Maximum bulk quantity cannot be negative."
                );
            }

            if (dto.getMaxBulkQuantity() > 0
                    && dto.getMaxBulkQuantity()
                    < dto.getBulkMinimumQuantity()) {

                throw new IllegalArgumentException(
                        "Maximum bulk quantity cannot be less than bulk minimum quantity."
                );
            }

            product.setBulkMinimumQuantity(
                    dto.getBulkMinimumQuantity()
            );

            product.setMaxBulkQuantity(
                    dto.getMaxBulkQuantity()
            );

            product.setBulkPricePerUnit(
                    dto.getBulkPricePerUnit()
            );

            // -------------------------------------------------
            // SELLING MODE
            // -------------------------------------------------

            String requestedMode =
                    dto.getSellingMode();

            if (requestedMode == null
                    || requestedMode.isBlank()) {

                requestedMode =
                        product.getSellingMode() == null
                                ? "INDEPENDENT"
                                : product.getSellingMode();
            }

            requestedMode =
                    requestedMode.trim().toUpperCase();

            if (!requestedMode.equals("INDEPENDENT")
                    && !requestedMode.equals("FPO")) {

                throw new IllegalArgumentException(
                        "Invalid selling mode."
                );
            }

            // -------------------------------------------------
            // INDEPENDENT
            // -------------------------------------------------

            if (requestedMode.equals("INDEPENDENT")) {

                if (product.getFpoProduct() != null) {

                    throw new IllegalArgumentException(
                            "This product is already part of an FPO bulk lot. It cannot be switched to independent sale until the FPO releases it."
                    );
                }

                product.setSellingMode(
                        "INDEPENDENT"
                );

                product.setFpo(null);

            } else {

                // -------------------------------------------------
                // FPO
                // -------------------------------------------------

                if (dto.getFpoId() == null) {

                    throw new IllegalArgumentException(
                            "Select an FPO for FPO contribution."
                    );
                }

                FPO selectedFpo =
                        fpoRepo.findById(
                                dto.getFpoId()
                        ).orElse(null);

                boolean memberOfSelectedFpo =
                        selectedFpo != null
                                && fpoRepo
                                .findByMembersContaining(farmer)
                                .stream()
                                .anyMatch(
                                        memberFpo ->
                                                memberFpo.getId()
                                                        == selectedFpo.getId()
                                );

                // IMPORTANT:
                // FPO.getId() returns primitive long.
                // Therefore == is correct.

                if (!memberOfSelectedFpo) {

                    throw new IllegalArgumentException(
                            "You can contribute only to an FPO where you are a member."
                    );
                }

                product.setSellingMode(
                        "FPO"
                );

                product.setFpo(
                        selectedFpo
                );
            }

            // -------------------------------------------------
            // UPDATE EXISTING FPO CONTRIBUTION
            // -------------------------------------------------

            if ("FPO".equalsIgnoreCase(
                    product.getSellingMode())) {

                FPOProductContribution contribution =
                        contributionRepo
                                .findBySourceProduct(product);

                if (contribution != null) {

                    int alreadySold =
                            contribution.getQuantityContributed()
                                    - contribution.getQuantityAvailable();

                    if (dto.getQuantity()
                            < alreadySold) {

                        throw new IllegalArgumentException(
                                "Quantity cannot be reduced below the amount already sold through the FPO ("
                                        + alreadySold
                                        + ")."
                        );
                    }

                    contribution.setQuantityContributed(
                            dto.getQuantity()
                    );

                    contribution.setQuantityAvailable(
                            dto.getQuantity()
                                    - alreadySold
                    );

                    contributionRepo.save(
                            contribution
                    );

                    recalculateFpoProduct(
                            contribution.getFpoProduct()
                    );
                }
            }

            // -------------------------------------------------
            // STATUS
            // -------------------------------------------------

            product.setStatus(
                    dto.getQuantity() > 0
                            ? "Available"
                            : "OutOfStock"
            );

            // -------------------------------------------------
            // IMAGE
            // -------------------------------------------------

            if (file != null
                    && !file.isEmpty()) {

                String storageFileName =
                        UUID.randomUUID()
                                + "_"
                                + file.getOriginalFilename();

                String uploadDir =
                        "Public/ProductImage/";

                Path uploadPath =
                        Paths.get(uploadDir);

                Files.createDirectories(
                        uploadPath
                );

                String oldImage =
                        product.getImage();

                try (InputStream inputStream =
                             file.getInputStream()) {

                    Files.copy(
                            inputStream,
                            uploadPath.resolve(
                                    storageFileName
                            ),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                product.setImage(
                        storageFileName
                );

                if (oldImage != null
                        && !oldImage.isBlank()) {

                    Files.deleteIfExists(
                            uploadPath.resolve(oldImage)
                    );
                }
            }

            // -------------------------------------------------
            // SAVE
            // -------------------------------------------------

            productRepo.save(product);

            attributes.addFlashAttribute(
                    "success",
                    "Product Successfully Updated"
            );

            return "redirect:/Former/EditProduct?id="
                    + product.getId();

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Error : " + e.getMessage()
            );

            return "redirect:/Former/EditProduct?id="
                    + dto.getId();
        }
    }


    // =========================================================
    // EDIT PRODUCT - PATH POST
    // =========================================================

    @PostMapping("/EditProduct/{id}")
    public String editProductByPath(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @ModelAttribute ProductDto dto,
            @RequestParam(
                    value = "file",
                    required = false
            ) MultipartFile file,
            RedirectAttributes attributes,
            HttpSession session) {

        dto.setId(id);

        return editProduct(
                dto,
                file,
                attributes,
                session
        );
    }


    // =========================================================
    // DELETE PRODUCT
    // =========================================================

    @GetMapping("/DeleteProduct")
    public String deleteProduct(
            @RequestParam("id") long id,
            HttpSession session,
            RedirectAttributes attributes) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        try {

            Optional<Product> optionalProduct =
                    productRepo.findById(id);

            if (optionalProduct.isEmpty()) {

                attributes.addFlashAttribute(
                        "error",
                        "Product not found."
                );

                return "redirect:/Former/ManageProduct";
            }

            Product product =
                    optionalProduct.get();

            if (!isOwner(product, farmer)) {

                attributes.addFlashAttribute(
                        "error",
                        "You are not authorized to delete this product."
                );

                return "redirect:/Former/ManageProduct";
            }

            String image =
                    product.getImage();

            productRepo.delete(product);

            if (image != null
                    && !image.isBlank()) {

                Files.deleteIfExists(
                        Paths.get(
                                "Public/ProductImage/"
                        ).resolve(image)
                );
            }

            attributes.addFlashAttribute(
                    "success",
                    "Product deleted successfully."
            );

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Unable to delete product: "
                            + e.getMessage()
            );
        }

        return "redirect:/Former/ManageProduct";
    }


    @PostMapping("/DeleteProduct/{id}")
    public String deleteProductByPost(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            HttpSession session,
            RedirectAttributes attributes) {

        return deleteProduct(
                id,
                session,
                attributes
        );
    }


    // =========================================================
    // FARMER ORDERS
    // =========================================================

    public List<Order> getSortedOrdersForFarmer(
            Formers farmer) {

        List<Order> orders =
                orderRepo.findByFarmerAndDemoOrder(
                        farmer,
                        false
                );

        orders.sort((o1, o2) -> {

            int statusRank1 =
                    getStatusRank(
                            o1.getOrderStatus()
                    );

            int statusRank2 =
                    getStatusRank(
                            o2.getOrderStatus()
                    );

            if (statusRank1 != statusRank2) {

                return Integer.compare(
                        statusRank1,
                        statusRank2
                );
            }

            if (o1.getOrderDate() == null
                    && o2.getOrderDate() == null) {

                return 0;
            }

            if (o1.getOrderDate() == null) {
                return 1;
            }

            if (o2.getOrderDate() == null) {
                return -1;
            }

            return o2.getOrderDate()
                    .compareTo(
                            o1.getOrderDate()
                    );
        });

        return orders;
    }


    private int getStatusRank(
            String status) {

        if (status == null) {
            return 4;
        }

        return switch (
                status.toLowerCase()) {

            case "confirmed" -> 1;
            case "delivered" -> 2;
            case "cancelled" -> 3;
            default -> 4;
        };
    }


    // =========================================================
    // DEMO ORDERS
    // =========================================================

    @GetMapping("/DemoOrders")
    public String showDemoOrders(
            Model model,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        try {

            demoAccessService
                    .requireFarmer(farmer);

        } catch (Exception e) {

            return "redirect:/Former/Dashboard";
        }

        model.addAttribute(
                "active4",
                "active"
        );

        model.addAttribute(
                "demoMode",
                true
        );

        model.addAttribute(
                "orders",
                orderRepo.findByFarmerAndDemoOrder(
                        farmer,
                        true
                )
        );

        Map<Long, FPOShipment> shipmentMap =
                new HashMap<>();

        for (FPOShipment shipment :
                shipmentRepo
                        .findByFarmerOrderByCreatedAtDesc(
                                farmer)) {

            if (shipment.getOrder() != null
                    && shipment.getOrder()
                    .isDemoOrder()) {

                shipmentMap.putIfAbsent(
                        shipment.getOrder().getOrderId(),
                        shipment
                );
            }
        }

        model.addAttribute(
                "shipmentMap",
                shipmentMap
        );

        return "Former/vieworders";
    }


    // =========================================================
    // ORDERS
    // =========================================================

    @GetMapping({"/Orders", "/orders"})
    public String showViewOrders(
            Model model,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        model.addAttribute(
                "active4",
                "active"
        );

        List<Order> orders =
                getSortedOrdersForFarmer(
                        farmer
                );

        model.addAttribute(
                "orders",
                orders
        );

        Map<Long, FPOShipment> shipmentMap =
                new HashMap<>();

        for (FPOShipment shipment :
                shipmentRepo
                        .findByFarmerOrderByCreatedAtDesc(
                                farmer)) {

            if (shipment.getOrder() != null
                    && shipment.getOrder()
                    .getOrderId() != null
                    && !shipmentMap.containsKey(
                            shipment.getOrder()
                                    .getOrderId())) {

                shipmentMap.put(
                        shipment.getOrder()
                                .getOrderId(),
                        shipment
                );
            }
        }

        model.addAttribute(
                "shipmentMap",
                shipmentMap
        );

        return "Former/vieworders";
    }


    // =========================================================
    // PAYMENT SLIP
    // =========================================================

    @GetMapping("/ViewPaymentSlip")
    public String viewPaymentSlip(
            @RequestParam("id") Long orderId,
            Model model,
            RedirectAttributes attributes,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        try {

            Order order =
                    orderRepo.findByOrderId(
                            orderId
                    );

            if (order == null
                    || order.getFarmer() == null
                    || order.getFarmer().getId()
                    != farmer.getId()) {

                attributes.addFlashAttribute(
                        "error",
                        "Order not found."
                );

                return "redirect:/Former/Orders";
            }

            Payment payment =
                    paymentRepo.findByOrder(
                            order
                    );

            if (payment == null) {

                attributes.addFlashAttribute(
                        "msg",
                        "No Payment Records Found"
                );

                return "redirect:/Former/Orders";
            }

            model.addAttribute(
                    "order",
                    order
            );

            model.addAttribute(
                    "payment",
                    payment
            );

            return "Former/ViewPaymentSlip";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "Error : " + e.getMessage()
            );

            return "redirect:/Former/Orders";
        }
    }


    // =========================================================
    // UPDATE ORDER STATUS
    // =========================================================

    @PostMapping("/updateOrderStatus")
    public String updateOrderStatus(
            @RequestParam("orderId") Long orderId,
            @RequestParam("newStatus") String newStatus,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        try {

            Order order =
                    orderRepo.findByOrderId(
                            orderId
                    );

            if (order == null
                    || order.getFarmer() == null
                    || order.getFarmer().getId()
                    != farmer.getId()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Order not found or unauthorized."
                );

                return "redirect:/Former/Orders";
            }

            if ("Delivered".equals(
                    order.getOrderStatus())) {

                redirectAttributes.addFlashAttribute(
                        "warning",
                        true
                );

                return "redirect:/Former/Orders";
            }

            if ("Cancelled".equals(
                    order.getOrderStatus())) {

                redirectAttributes.addFlashAttribute(
                        "Cancelled",
                        true
                );

                return "redirect:/Former/Orders";
            }

            Payment payment =
                    paymentRepo.findByOrder(
                            order
                    );

            if ("Delivered".equalsIgnoreCase(
                    newStatus)) {

                order.setDeliveredDate(
                        LocalDateTime.now()
                );
            }

            if ("Cancelled".equalsIgnoreCase(
                    newStatus)) {

                if (payment != null) {

                    sendAutoEmail
                            .SendOrderCancellationEmail(
                                    order,
                                    payment
                            );

                    if (payment.getPayId() != null
                            && !payment.getPayId()
                            .isBlank()) {

                        paymentService.refundPayment(
                                payment.getPayId()
                        );
                    }
                }
            }

            order.setOrderStatus(
                    newStatus
            );

            orderRepo.save(order);

            redirectAttributes.addFlashAttribute(
                    "success",
                    true
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to update order: "
                            + e.getMessage()
            );
        }

        return "redirect:/Former/Orders";
    }


    // =========================================================
    // PROFILE
    // =========================================================

    @GetMapping("/UserProfile")
    public String showViewProfile(
            Model model,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return "redirect:/Flogin";
        }

        model.addAttribute(
                "active6",
                "active"
        );

        model.addAttribute(
                "former",
                farmer
        );

        return "Former/viewprofile";
    }


    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @GetMapping("/ChangePassword")
    public String showChangePassword(
            HttpSession session) {

        return getLoggedInFormer(session) != null
                ? "Former/ChangePassword"
                : "redirect:/Flogin";
    }


    @PostMapping("/ChangePassword")
    public String changePassword(
            HttpServletRequest request,
            RedirectAttributes attributes,
            HttpSession session) {

        try {

            Formers farmer =
                    getLoggedInFormer(session);

            if (farmer == null) {
                return "redirect:/Flogin";
            }

            String newPassword =
                    request.getParameter("newpass");

            String confirmPassword =
                    request.getParameter("confirmpass");

            String oldPassword =
                    request.getParameter("oldpass");

            if (!newPassword.equals(
                    confirmPassword)) {

                attributes.addFlashAttribute(
                        "error",
                        "New password and Confirm Password do not match."
                );

                return "redirect:/Former/ChangePassword";
            }

            if (!farmer.getPassword().equals(
                    oldPassword)) {

                attributes.addFlashAttribute(
                        "error",
                        "Invalid Old Password."
                );

                return "redirect:/Former/ChangePassword";
            }

            farmer.setPassword(
                    confirmPassword
            );

            formersRepo.save(farmer);

            session.invalidate();

            attributes.addFlashAttribute(
                    "msg",
                    "Password Successfully Changed"
            );

            return "redirect:/Flogin";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Error : " + e.getMessage()
            );

            return "redirect:/Former/ChangePassword";
        }
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @GetMapping("/Logout")
    public String logout(
            RedirectAttributes attributes,
            HttpSession session) {

        if (getLoggedInFormer(session) == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Session Expired ⚠️"
            );

            return "redirect:/Flogin";
        }

        session.removeAttribute(
                "loggedInFormer"
        );

        session.removeAttribute(
                "former"
        );

        attributes.addFlashAttribute(
                "msg",
                "Logout Successful ✅"
        );

        return "redirect:/Flogin";
    }


    // =========================================================
    // SHARED FORMER HEADER DATA
    // =========================================================

    @ModelAttribute
    public void addGlobalAttributes(
            Model model,
            HttpSession session) {

        Formers farmer =
                getLoggedInFormer(session);

        if (farmer == null) {
            return;
        }

        List<Order> pendingOrders =
                orderRepo
                        .findTop4ByFarmerAndOrderStatusOrderByOrderDateDesc(
                                farmer,
                                "Confirmed"
                        );

        model.addAttribute(
                "pendingOrderCount",
                orderRepo
                        .countByFarmerAndDemoOrderAndOrderStatus(
                                farmer,
                                false,
                                "Confirmed"
                        )
        );

        model.addAttribute(
                "pendingOrders",
                pendingOrders
        );

        model.addAttribute(
                "former",
                farmer
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private void recalculateFpoProduct(
            com.softpro.ATH.Model.FPOProduct fpoProduct) {

        if (fpoProduct == null) {
            return;
        }

        List<FPOProductContribution> contributions =
                contributionRepo.findByFpoProduct(
                        fpoProduct
                );

        int total =
                contributions.stream()
                        .mapToInt(
                                FPOProductContribution
                                        ::getQuantityContributed
                        )
                        .sum();

        int available =
                contributions.stream()
                        .mapToInt(
                                FPOProductContribution
                                        ::getQuantityAvailable
                        )
                        .sum();

        fpoProduct.setTotalQuantity(
                total
        );

        fpoProduct.setAvailableQuantity(
                available
        );

        fpoProduct.setStatus(
                available > 0
                        ? "Available"
                        : "OutOfStock"
        );
    }


    private Formers getLoggedInFormer(
            HttpSession session) {

        Object loggedInFormer =
                session.getAttribute(
                        "loggedInFormer"
                );

        if (loggedInFormer instanceof Formers) {

            return (Formers) loggedInFormer;
        }

        Object former =
                session.getAttribute(
                        "former"
                );

        if (former instanceof Formers) {

            Formers farmer =
                    (Formers) former;

            session.setAttribute(
                    "loggedInFormer",
                    farmer
            );

            return farmer;
        }

        return null;
    }


    private boolean isOwner(
            Product product,
            Formers farmer) {

        return product != null
                && product.getFarmer() != null
                && product.getFarmer().getId()
                == farmer.getId();
    }


    // =========================================================
    // FAIR PRICE API
    // =========================================================

    @GetMapping("/fair-price")
    @ResponseBody
    public com.softpro.ATH.Dto.FairPriceResponse fairPrice(
            @RequestParam String productName,
            @RequestParam(required = false) String category,
            @RequestParam BigDecimal price) {

        return fairPriceService.checkPrice(
                productName,
                category,
                price
        );
    }
}

