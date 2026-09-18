package com.softpro.ATH.Controller;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
import com.softpro.ATH.Model.Category;
import com.softpro.ATH.Model.FPOProduct;
import com.softpro.ATH.Model.FPOProductContribution;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.OrderFarmerContribution;
import com.softpro.ATH.Model.Payment;
import com.softpro.ATH.Model.Product;
import com.softpro.ATH.Model.FPOShipment;
import com.softpro.ATH.Repository.CategoryRepo;
import com.softpro.ATH.Repository.FPOProductContributionRepo;
import com.softpro.ATH.Repository.FPOProductRepo;
import com.softpro.ATH.Repository.MerchantRepo;
import com.softpro.ATH.Repository.OrderFarmerContributionRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.PaymentRepo;
import com.softpro.ATH.Repository.ProductRepo;
import com.softpro.ATH.Repository.FPOShipmentRepo;
import com.softpro.ATH.Service.DemoAccessService;
import com.softpro.ATH.Service.LogisticsCostService;
import com.softpro.ATH.Service.FairPriceService;
import com.softpro.ATH.Dto.LogisticsCostResponse;
import com.softpro.ATH.Dto.MarketplaceOfferDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Merchant")
public class MerchantController {

    @Autowired private ProductRepo productRepo;
    @Autowired private MerchantRepo merchantRepo;
    @Autowired private OrderRepo orderRepo;
    @Autowired private HttpSession session;
    @Autowired private CategoryRepo categoryRepo;
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepo paymentRepo;
    @Autowired private SendAutoEmail sendAutoEmail;
    @Autowired private FPOProductRepo fpoProductRepo;
    @Autowired private FPOProductContributionRepo contributionRepo;
    @Autowired private OrderFarmerContributionRepo orderFarmerContributionRepo;

    // Shipment lookup used only to expose shipment state on My Orders.
    // Existing merchant features remain unchanged.
    @Autowired private FPOShipmentRepo shipmentRepo;
    @Autowired private DemoAccessService demoAccessService;
    @Autowired private LogisticsCostService logisticsCostService;
    @Autowired private FairPriceService fairPriceService;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @GetMapping("/Dashboard")
    public String ShowDashboard(RedirectAttributes attributes, Model model,
            @RequestParam(value = "category", required = false) String categoryName) {

        if (session.getAttribute("loggedInMerchant") == null) {
            attributes.addFlashAttribute("error", "Session Expired ⚠️");
            return "redirect:/Mlogin";
        }

        List<Category> categories = categoryRepo.findAll();
        List<Product> products;

        if (categoryName != null && !categoryName.isEmpty()) {

            products = productRepo.findByCategoryAndStatus(
                            categoryName,
                            "Available"
                    )
                    .stream()
                    .filter(p ->
                            p.getSellingMode() == null
                                    || "INDEPENDENT"
                                    .equalsIgnoreCase(
                                            p.getSellingMode()
                                    )
                    )
                    .toList();

            model.addAttribute(
                    "selectedCategory",
                    categoryName
            );

        } else {

            products =
                    productRepo.findMarketplaceProducts(
                            "Available",
                            "INDEPENDENT"
                    );
        }

        model.addAttribute("categories", categories);
        model.addAttribute("products", products);
        model.addAttribute(
                "bulkProducts",
                fpoProductRepo.findByStatus("Available")
        );

        List<String> comparisonProducts = new ArrayList<>();
        productRepo.findMarketplaceProducts("Available", "INDEPENDENT")
                .stream().map(Product::getProductName).filter(n -> n != null && !n.isBlank())
                .forEach(comparisonProducts::add);
        fpoProductRepo.findByStatus("Available")
                .stream().map(FPOProduct::getProductName).filter(n -> n != null && !n.isBlank())
                .forEach(comparisonProducts::add);
        comparisonProducts = comparisonProducts.stream().distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        model.addAttribute("comparisonProducts", comparisonProducts);

        return "Merchant/dashboard";
    }


    @GetMapping("/BuyBulkProduct")
    public String showBuyBulkProduct(
            @RequestParam("id") Long id,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute("loggedInMerchant") == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Session Expired ⚠️"
            );
            return "redirect:/Mlogin";
        }

        try {

            FPOProduct product =
                    fpoProductRepo.findById(id).orElseThrow();

            if (!"Available".equalsIgnoreCase(
                    product.getStatus())
                    || product.getAvailableQuantity() <= 0) {

                attributes.addFlashAttribute(
                        "error",
                        "This bulk product is currently unavailable."
                );

                return "redirect:/Merchant/Dashboard";
            }

            model.addAttribute("product", product);
            model.addAttribute(
                    "razorpayKeyId",
                    "rzp_live_Io1s9ctQtD0G1b"  
            );

            return "Merchant/BuyBulkProduct";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Unable to open bulk product: "
                            + e.getMessage()
            );

            return "redirect:/Merchant/Dashboard";
        }
    }


    @GetMapping("/create-bulk-order")
    @ResponseBody
    public Map<String, Object> createBulkRazorpayOrder(@RequestParam long productId,
                                                        @RequestParam int quantity) {
        Map<String, Object> data = new HashMap<>();
        try {
            Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
            if (merchant == null) throw new IllegalArgumentException("Session expired. Please login again.");
            FPOProduct product = fpoProductRepo.findById(productId).orElseThrow();
            validateBulkQuantity(product, quantity);
            LogisticsCostResponse logistics = logisticsCostService.calculateFromAddresses(
                    quantity, product.getFpo() == null ? null : product.getFpo().getAddress(), merchant.getAddress());
            if (!logistics.isSuccess()) throw new IllegalArgumentException(logistics.getMessage());
            BigDecimal productAmount = product.getPricePerUnit().multiply(BigDecimal.valueOf(quantity));
            BigDecimal total = productAmount.add(logistics.getLogisticsCost()).setScale(2, java.math.RoundingMode.HALF_UP);
            com.razorpay.Order razorOrder = paymentService.createRazorpayOrder(total);
            data.put("orderId", razorOrder.get("id"));
            data.put("razorpayKeyId", "rzp_live_Io1s9ctQtD0G1b");
            data.put("amount", total.movePointRight(2).longValueExact());
            data.put("currency", "INR");
            data.put("productAmount", productAmount);
            data.put("logisticsAmount", logistics.getLogisticsCost());
            data.put("totalAmount", total);
            data.put("roadDistanceKm", logistics.getRoadDistanceKm());
            data.put("vehicleType", logistics.getVehicleType().name());
        } catch (Exception e) { data.put("error", e.getMessage()); }
        return data;
    }

    @PostMapping("/verify_bulk_payment")
    @Transactional
    public String verifyBulkPayment(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("orderId") String razorpayOrderId,
            @RequestParam("signature") String signature,
            @RequestParam("productId") long productId,
            @RequestParam("buyQuantity") int quantity,
            RedirectAttributes attributes) {

        try {

            Merchant merchant =
                    (Merchant) session.getAttribute(
                            "loggedInMerchant"
                    );

            if (merchant == null) {
                return "redirect:/Mlogin";
            }

            FPOProduct product =
                    fpoProductRepo.findById(
                            productId
                    ).orElseThrow();
            validateBulkQuantity(product, quantity);

            if (!"Available".equalsIgnoreCase(
                    product.getStatus())
                    || quantity <
                            product.getMinimumBulkQuantity()
                    || quantity >
                            product.getAvailableQuantity()) {

                throw new IllegalArgumentException(
                        "Bulk product is unavailable or quantity is invalid."
                );
            }

            List<FPOProductContribution>
                    configuredContributions =
                    contributionRepo.findByFpoProduct(
                            product
                    );

            if (!configuredContributions.isEmpty()) {

                int contributionStock =
                        configuredContributions
                                .stream()
                                .mapToInt(
                                        FPOProductContribution
                                                ::getQuantityAvailable
                                )
                                .sum();

                if (contributionStock < quantity) {

                    throw new IllegalArgumentException(
                            "This bulk product does not have enough farmer-attributed stock for the requested quantity."
                    );
                }
            }

            Order order =
                    new Order();

            order.setProductName(
                    product.getProductName()
            );

            order.setPricePerUnit(
                    product.getPricePerUnit()
            );

            order.setQuantity(
                    quantity
            );

            order.setMerchant(
                    merchant
            );

            order.setFpo(
                    product.getFpo()
            );

            order.setFpoProduct(
                    product
            );

            order.setOrderStatus(
                    "Pending"
            );

            order.setOrderDate(
                    LocalDateTime.now()
            );

            orderRepo.save(order);


            Payment payment =
                    new Payment();

            payment.setOrder(
                    order
            );

            long transactionId =
                    System.currentTimeMillis();

            payment.setTransactionId(
                    transactionId
            );

            payment.setPayId(
                    paymentId
            );

            BigDecimal productAmount = product.getPricePerUnit().multiply(BigDecimal.valueOf(quantity));
            LogisticsCostResponse logistics = logisticsCostService.calculateFromAddresses(
                    quantity, product.getFpo().getAddress(), merchant.getAddress());
            if (!logistics.isSuccess()) throw new IllegalArgumentException(logistics.getMessage());
            BigDecimal totalAmount = productAmount.add(logistics.getLogisticsCost()).setScale(2, java.math.RoundingMode.HALF_UP);
            order.setProductAmount(productAmount);
            order.setLogisticsAmount(logistics.getLogisticsCost());
            order.setTotalAmount(totalAmount);
            order.setRoadDistanceKm(logistics.getRoadDistanceKm());
            order.setVehicleType(logistics.getVehicleType().name());
            payment.setAmount(totalAmount);
            payment.setProductAmount(productAmount);
            payment.setLogisticsAmount(logistics.getLogisticsCost());
            payment.setTotalAmount(totalAmount);

            payment.setPaymentMode(
                    "Online"
            );

            payment.setPaymentSource("RAZORPAY");

            payment.setPaymentDate(
                    LocalDateTime.now()
            );

            paymentRepo.save(
                    payment
            );


            List<FPOProductContribution>
                    contributions =
                    contributionRepo.findByFpoProduct(
                            product
                    );

            if (!contributions.isEmpty()) {

                int remainingToAllocate =
                        quantity;

                for (FPOProductContribution contribution :
                        contributions) {

                    if (remainingToAllocate <= 0) {
                        break;
                    }

                    int available =
                            contribution
                                    .getQuantityAvailable();

                    if (available <= 0) {
                        continue;
                    }

                    int allocated =
                            Math.min(
                                    available,
                                    remainingToAllocate
                            );

                    contribution.setQuantityAvailable(
                            available - allocated
                    );

                    contributionRepo.save(
                            contribution
                    );

                    Product sourceProduct =
                            contribution.getSourceProduct();

                    if (sourceProduct != null) {

                        int sourceRemaining =
                                Math.max(
                                        0,
                                        sourceProduct.getQuantity()
                                                - allocated
                                );

                        sourceProduct.setQuantity(
                                sourceRemaining
                        );

                        sourceProduct.setStatus(
                                sourceRemaining > 0
                                        ? "Available"
                                        : "OutOfStock"
                        );

                        productRepo.save(
                                sourceProduct
                        );
                    }

                    OrderFarmerContribution allocation =
                            new OrderFarmerContribution();

                    allocation.setOrder(
                            order
                    );

                    allocation.setFarmer(
                            contribution.getFarmer()
                    );

                    allocation.setQuantity(
                            allocated
                    );

                    BigDecimal farmerRate =
                            sourceProduct != null
                                    && sourceProduct
                                            .getPricePerUnit() != null
                                    ? sourceProduct
                                            .getPricePerUnit()
                                    : BigDecimal.ZERO;

                    allocation.setAmount(
                            farmerRate.multiply(
                                    BigDecimal.valueOf(
                                            allocated
                                    )
                            )
                    );

                    orderFarmerContributionRepo.save(
                            allocation
                    );

                    sendAutoEmail
                            .SendBulkFarmerContributionEmail(
                                    order,
                                    allocation,
                                    "Paid"
                            );

                    remainingToAllocate -=
                            allocated;
                }

                if (remainingToAllocate > 0) {

                    throw new IllegalArgumentException(
                            "Configured farmer contributions do not have enough available quantity for this order."
                    );
                }
            }


            int remaining =
                    product.getAvailableQuantity()
                            - quantity;

            product.setAvailableQuantity(
                    remaining
            );

            if (remaining <= 0) {
                product.setStatus(
                        "OutOfStock"
                );
            }

            fpoProductRepo.save(
                    product
            );

            sendAutoEmail
                    .SendBulkOrderPlacedEmail(
                            order
                    );

            attributes.addFlashAttribute(
                    "bulkPaid",
                    true
            );

            attributes.addFlashAttribute(
                    "transactionId",
                    transactionId
            );

            attributes.addFlashAttribute(
                    "quantity",
                    quantity
            );

            return "redirect:/Merchant/BuyBulkProduct?id="
                    + productId;

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Bulk payment verification failed: "
                            + e.getMessage()
            );

            return "redirect:/Merchant/BuyBulkProduct?id="
                    + productId;
        }
    }


    @GetMapping("/ViewProfile")
    public String ShowViewProfile(
            Model model) {

        if (session.getAttribute(
                "loggedInMerchant") != null) {

            model.addAttribute(
                    "merchant",
                    (Merchant) session.getAttribute(
                            "loggedInMerchant"
                    )
            );

            return "Merchant/ViewProfile";
        }

        return "redirect:/Mlogin";
    }


    @GetMapping("/EditProfile")
    public String ShowEditProfile(
            Model model,
            HttpSession session) {

        if (session.getAttribute(
                "loggedInMerchant") == null) {

            return "redirect:/Mlogin";
        }

        model.addAttribute(
                "merchant",
                (Merchant) session.getAttribute(
                        "loggedInMerchant"
                )
        );

        return "Merchant/EditProfile";
    }


    @PostMapping("/UpdateProfile")
    public String UpdateProfilePic(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute("merchant") Merchant oldMerchant,
            RedirectAttributes attributes,
            HttpSession session) {

        try {

            Merchant merchant =
                    (Merchant) session.getAttribute(
                            "loggedInMerchant"
                    );

            if (merchant == null) {
                return "redirect:/Mlogin";
            }

            if (file != null
                    && !file.isEmpty()) {

                String storageFileName =
                        System.currentTimeMillis()
                                + "_"
                                + file.getOriginalFilename();

                String uploadDir =
                        "public/ProfilePic/";

                Path uploadPath =
                        Paths.get(
                                uploadDir
                        );

                if (!Files.exists(
                        uploadPath)) {

                    Files.createDirectories(
                            uploadPath
                    );
                }

                String oldProfilePic =
                        merchant.getProfilepic();

                if (oldProfilePic != null
                        && !oldProfilePic.isEmpty()) {

                    Path oldFilePath =
                            Paths.get(
                                    uploadDir
                                            + oldProfilePic
                            );

                    if (Files.exists(
                            oldFilePath)) {

                        Files.delete(
                                oldFilePath
                        );
                    }
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

                merchant.setProfilepic(
                        storageFileName
                );
            }

            merchant.setName(
                    oldMerchant.getName()
            );

            merchant.setContactno(
                    oldMerchant.getContactno()
            );

            merchant.setAadharno(
                    oldMerchant.getAadharno()
            );

            merchant.setAddress(
                    oldMerchant.getAddress()
            );

            merchant.setPancardno(
                    oldMerchant.getPancardno()
            );

            merchantRepo.save(
                    merchant
            );

            attributes.addFlashAttribute(
                    "msg",
                    "Profile Successfully Updated"
            );

            return "redirect:/Merchant/EditProfile";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "msg",
                    "Error : " + e.getMessage()
            );

            return "redirect:/Merchant/EditProfile";
        }
    }


    @GetMapping("/BuyProduct")
    public String ShowBuyProduct(
            @RequestParam("id") long id,
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute(
                "loggedInMerchant") == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Session Expired ⚠️"
            );

            return "redirect:/Mlogin";
        }

        Product product =
                productRepo.findById(
                        id
                ).orElseThrow();

        model.addAttribute(
                "razorpayKeyId",
                "rzp_live_Io1s9ctQtD0G1b"
        );

        model.addAttribute(
                "product",
                product
        );

        return "Merchant/BuyProduct";
    }


    @GetMapping("/create-order")
    @ResponseBody
    public Map<String, Object> createRazorpayOrder(@RequestParam long productId,
                                                    @RequestParam int quantity) {
        Map<String, Object> data = new HashMap<>();
        try {
            Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
            if (merchant == null) throw new IllegalArgumentException("Session expired. Please login again.");
            Product product = productRepo.findById(productId).orElseThrow();
            validateNormalQuantity(product, quantity);
            LogisticsCostResponse logistics = logisticsCostService.calculateFromAddresses(
                    quantity, product.getFarmer().getAddress(), merchant.getAddress());
            if (!logistics.isSuccess()) throw new IllegalArgumentException(logistics.getMessage());
            BigDecimal productAmount = product.getPricePerUnit().multiply(BigDecimal.valueOf(quantity));
            BigDecimal total = productAmount.add(logistics.getLogisticsCost()).setScale(2, java.math.RoundingMode.HALF_UP);
            com.razorpay.Order razorOrder = paymentService.createRazorpayOrder(total);
            data.put("orderId", razorOrder.get("id"));
            data.put("razorpayKeyId", "rzp_live_Io1s9ctQtD0G1b");
            data.put("amount", total.movePointRight(2).longValueExact());
            data.put("currency", "INR");
            data.put("productAmount", productAmount);
            data.put("logisticsAmount", logistics.getLogisticsCost());
            data.put("totalAmount", total);
            data.put("roadDistanceKm", logistics.getRoadDistanceKm());
            data.put("vehicleType", logistics.getVehicleType().name());
        } catch (Exception e) { data.put("error", e.getMessage()); }
        return data;
    }

    @GetMapping("/estimate-logistics")
    @ResponseBody
    public Map<String,Object> estimateLogistics(@RequestParam long productId, @RequestParam int quantity,
                                                 @RequestParam(defaultValue="FARMER") String source) {
        Map<String,Object> data = new HashMap<>();
        try {
            Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
            if (merchant == null) throw new IllegalArgumentException("Session expired.");
            String pickup;
            BigDecimal unitPrice;
            if ("FPO".equalsIgnoreCase(source)) {
                FPOProduct p = fpoProductRepo.findById(productId).orElseThrow();
                validateBulkQuantity(p, quantity);
                pickup = p.getFpo().getAddress();
                unitPrice = p.getPricePerUnit();
            } else {
                Product p = productRepo.findById(productId).orElseThrow();
                validateNormalQuantity(p, quantity);
                pickup = p.getFarmer().getAddress();
                unitPrice = p.getPricePerUnit();
            }
            LogisticsCostResponse l = logisticsCostService.calculateFromAddresses(quantity, pickup, merchant.getAddress());
            if (!l.isSuccess()) throw new IllegalArgumentException(l.getMessage());
            BigDecimal productAmount=unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal total=productAmount.add(l.getLogisticsCost()).setScale(2, java.math.RoundingMode.HALF_UP);
            data.put("success",true); data.put("productAmount",productAmount);
            data.put("logisticsAmount",l.getLogisticsCost()); data.put("totalAmount",total);
            data.put("roadDistanceKm",l.getRoadDistanceKm()); data.put("vehicleType",l.getVehicleType().name());
        } catch(Exception e) { data.put("success",false); data.put("error",e.getMessage()); }
        return data;
    }

    @GetMapping("/compare-offers")
    @ResponseBody
    public Map<String,Object> compareOffers(@RequestParam(defaultValue="1") int quantity, @RequestParam(required=false) String productName) {
        Map<String,Object> data=new HashMap<>();
        try {
            Merchant merchant=(Merchant)session.getAttribute("loggedInMerchant");
            if(merchant==null) throw new IllegalArgumentException("Session expired.");
            if(productName==null || productName.isBlank()) throw new IllegalArgumentException("Select a product to compare.");
            productName=productName.trim();
            List<MarketplaceOfferDto> offers=new java.util.ArrayList<>();
            // Independent farmer offers
            for(Product p: productRepo.findMarketplaceProducts("Available","INDEPENDENT")) {
                try {
                    validateNormalQuantity(p,quantity);
                    if (p.getFarmer() == null || p.getProductName() == null || !p.getProductName().equalsIgnoreCase(productName)) continue;
                    LogisticsCostResponse l=logisticsCostService.calculateFromAddresses(quantity,p.getFarmer().getAddress(),merchant.getAddress());
                    if(!l.isSuccess()) continue;
                    MarketplaceOfferDto o=new MarketplaceOfferDto();
                    o.setProductId(p.getId()); o.setProductName(p.getProductName()); o.setCategory(p.getCategory());
                    o.setSellerName(p.getFarmer().getName()); o.setSellerType("FARMER"); o.setPurchaseType("FARMER"); o.setProductPrice(p.getPricePerUnit());
                    o.setAvailableQuantity(p.getQuantity()); o.setRoadDistanceKm(l.getRoadDistanceKm());
                    o.setVehicleType(l.getVehicleType()); o.setLogisticsCost(l.getLogisticsCost());
                    o.setLandedCost(p.getPricePerUnit().add(l.getLogisticsCost().divide(BigDecimal.valueOf(quantity),2,java.math.RoundingMode.HALF_UP)));
                    com.softpro.ATH.Dto.FairPriceResponse fair = fairPriceService.checkPrice(p.getProductName(), p.getCategory(), p.getPricePerUnit());
                    o.setFairPriceStatus(fair.getStatus()); o.setMarketReferencePrice(fair.getReferencePrice());
                    o.setLowerFairPrice(fair.getLowerFairPrice()); o.setUpperFairPrice(fair.getUpperFairPrice());
                    offers.add(o);
                } catch(Exception ignored) {}
            }

            // FPO bulk offers
            for(FPOProduct p: fpoProductRepo.findByStatus("Available")) {
                try {
                    validateBulkQuantity(p,quantity);
                    if (p.getFpo() == null || p.getProductName() == null || !p.getProductName().equalsIgnoreCase(productName)) continue;
                    LogisticsCostResponse l=logisticsCostService.calculateFromAddresses(quantity,p.getFpo().getAddress(),merchant.getAddress());
                    if(!l.isSuccess()) continue;
                    MarketplaceOfferDto o=new MarketplaceOfferDto();
                    o.setProductId(p.getId()); o.setProductName(p.getProductName()); o.setCategory(p.getCategory());
                    o.setSellerName(p.getFpo().getName()); o.setSellerType("FPO"); o.setPurchaseType("FPO"); o.setProductPrice(p.getPricePerUnit());
                    o.setAvailableQuantity(p.getAvailableQuantity()); o.setRoadDistanceKm(l.getRoadDistanceKm());
                    o.setVehicleType(l.getVehicleType()); o.setLogisticsCost(l.getLogisticsCost());
                    o.setLandedCost(p.getPricePerUnit().add(l.getLogisticsCost().divide(BigDecimal.valueOf(quantity),2,java.math.RoundingMode.HALF_UP)));
                    com.softpro.ATH.Dto.FairPriceResponse fair = fairPriceService.checkPrice(p.getProductName(), p.getCategory(), p.getPricePerUnit());
                    o.setFairPriceStatus(fair.getStatus()); o.setMarketReferencePrice(fair.getReferencePrice());
                    o.setLowerFairPrice(fair.getLowerFairPrice()); o.setUpperFairPrice(fair.getUpperFairPrice());
                    offers.add(o);
                } catch(Exception ignored) {}
            }
            offers.sort(Comparator.comparing(MarketplaceOfferDto::getLandedCost));
            BigDecimal best=offers.stream().map(MarketplaceOfferDto::getLandedCost).min(BigDecimal::compareTo).orElse(null);
            if(best!=null) offers.forEach(o->o.setBestLandedCost(o.getLandedCost().compareTo(best)==0));
            data.put("offers",offers); data.put("quantity",quantity); data.put("productName",productName);
        }catch(Exception e){data.put("error",e.getMessage());}
        return data;
    }

    private void validateNormalQuantity(Product p,int quantity){
        if(p==null || !"Available".equalsIgnoreCase(p.getStatus())) throw new IllegalArgumentException("Product is unavailable.");
        if(quantity<=0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        if(quantity<p.getMinimumOrderQuantity()) throw new IllegalArgumentException("Minimum order quantity is "+p.getMinimumOrderQuantity()+".");
        if(p.getMaxOrderQuantity()>0 && quantity>p.getMaxOrderQuantity()) throw new IllegalArgumentException("Maximum order quantity is "+p.getMaxOrderQuantity()+".");
        if(quantity>p.getQuantity()) throw new IllegalArgumentException("Requested quantity is not available.");
    }

    private void validateBulkQuantity(FPOProduct p,int quantity){
        if(p==null || !"Available".equalsIgnoreCase(p.getStatus())) throw new IllegalArgumentException("Bulk product is unavailable.");
        if(quantity<=0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        if(quantity<p.getMinimumBulkQuantity()) throw new IllegalArgumentException("Minimum bulk quantity is "+p.getMinimumBulkQuantity()+".");
        if(p.getMaxBulkQuantity()>0 && quantity>p.getMaxBulkQuantity()) throw new IllegalArgumentException("Maximum bulk quantity is "+p.getMaxBulkQuantity()+".");
        if(quantity>p.getAvailableQuantity()) throw new IllegalArgumentException("Requested quantity is not available.");
    }

    @PostMapping("/verify_payment")
    public String verifyPayment(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("orderId") String razorpayOrderId,
            @RequestParam("signature") String signature,
            @RequestParam("productId") long productId,
            @RequestParam("buyQuantity") int quantity,
            Model model,
            RedirectAttributes attributes) {

        try {

            Merchant merchant =
                    (Merchant) session.getAttribute(
                            "loggedInMerchant"
                    );

            Product product =
                    productRepo.findById(
                            productId
                    ).orElseThrow();
            validateNormalQuantity(product, quantity);

            Order order =
                    new Order();

            order.setProductName(
                    product.getProductName()
            );

            order.setPricePerUnit(
                    product.getPricePerUnit()
            );

            order.setQuantity(
                    quantity
            );

            order.setFarmer(
                    product.getFarmer()
            );

            order.setMerchant(
                    merchant
            );

            order.setOrderStatus(
                    "Confirmed"
            );

            order.setOrderDate(
                    LocalDateTime.now()
            );

            orderRepo.save(
                    order
            );

            Payment payment =
                    new Payment();

            payment.setOrder(
                    order
            );

            long transactionId =
                    System.currentTimeMillis();

            payment.setTransactionId(
                    transactionId
            );

            payment.setPayId(
                    paymentId
            );

            BigDecimal productAmount = product.getPricePerUnit().multiply(BigDecimal.valueOf(quantity));
            LogisticsCostResponse logistics = logisticsCostService.calculateFromAddresses(
                    quantity, product.getFarmer().getAddress(), merchant.getAddress());
            if (!logistics.isSuccess()) throw new IllegalArgumentException(logistics.getMessage());
            BigDecimal totalAmount = productAmount.add(logistics.getLogisticsCost()).setScale(2, java.math.RoundingMode.HALF_UP);
            order.setProductAmount(productAmount);
            order.setLogisticsAmount(logistics.getLogisticsCost());
            order.setTotalAmount(totalAmount);
            order.setRoadDistanceKm(logistics.getRoadDistanceKm());
            order.setVehicleType(logistics.getVehicleType().name());
            payment.setAmount(totalAmount);
            payment.setProductAmount(productAmount);
            payment.setLogisticsAmount(logistics.getLogisticsCost());
            payment.setTotalAmount(totalAmount);

            payment.setPaymentMode(
                    "Online"
            );

            payment.setPaymentSource("RAZORPAY");

            payment.setPaymentDate(
                    LocalDateTime.now()
            );

            paymentRepo.save(
                    payment
            );

            int remainingQty =
                    product.getQuantity()
                            - order.getQuantity();

            if (remainingQty <= 0) {

                remainingQty = 0;

                product.setStatus(
                        "OutOfStock"
                );
            }

            product.setQuantity(
                    remainingQty
            );

            productRepo.save(
                    product
            );

            sendAutoEmail
                    .SendOrderConfirmationEmail(
                            order
                    );

            attributes.addFlashAttribute(
                    "msg",
                    true
            );

            attributes.addFlashAttribute(
                    "transactionId",
                    transactionId
            );

            attributes.addFlashAttribute(
                    "quantity",
                    quantity
            );

            return "redirect:/Merchant/BuyProduct?id="
                    + productId;

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Payment verification failed: "
                            + e.getMessage()
            );

            return "redirect:/Merchant/BuyProduct?id="
                    + productId;
        }
    }


    // Demo orders intentionally bypass Razorpay but use the same downstream Order -> confirm -> shipment pipeline.
    @PostMapping("/demo-order")
    @Transactional
    public String createDemoOrder(
            @RequestParam long productId,
            @RequestParam int buyQuantity,
            @RequestParam(defaultValue = "FARMER") String source,
            RedirectAttributes attributes) {

        Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
        if (merchant == null) {
            return "redirect:/Mlogin";
        }

        try {
            demoAccessService.requireMerchant(merchant);

            if (buyQuantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero.");
            }

            Order order = new Order();
            order.setMerchant(merchant);
            order.setQuantity(buyQuantity);
            order.setOrderDate(LocalDateTime.now());
            order.setDemoOrder(true);

            if ("FPO".equalsIgnoreCase(source)) {
                FPOProduct product = fpoProductRepo.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("FPO product not found."));

                validateBulkQuantity(product, buyQuantity);

                order.setProductName(product.getProductName());
                order.setPricePerUnit(product.getPricePerUnit());
                order.setFpo(product.getFpo());
                order.setFpoProduct(product);
                order.setOrderStatus("Pending");
            } else {
                Product product = productRepo.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Product not found."));

                validateNormalQuantity(product, buyQuantity);

                order.setProductName(product.getProductName());
                order.setPricePerUnit(product.getPricePerUnit());
                order.setFarmer(product.getFarmer());
                order.setOrderStatus("Confirmed");
            }

            String pickupAddress = "FPO".equalsIgnoreCase(source)
                    ? order.getFpo().getAddress()
                    : order.getFarmer().getAddress();
            LogisticsCostResponse logistics = logisticsCostService.calculateFromAddresses(
                    buyQuantity, pickupAddress, merchant.getAddress());
            if (!logistics.isSuccess()) {
                throw new IllegalArgumentException(logistics.getMessage());
            }
            BigDecimal productAmount = order.getPricePerUnit().multiply(BigDecimal.valueOf(buyQuantity));
            BigDecimal totalAmount = productAmount.add(logistics.getLogisticsCost()).setScale(2, java.math.RoundingMode.HALF_UP);
            order.setProductAmount(productAmount);
            order.setLogisticsAmount(logistics.getLogisticsCost());
            order.setTotalAmount(totalAmount);
            order.setRoadDistanceKm(logistics.getRoadDistanceKm());
            order.setVehicleType(logistics.getVehicleType().name());

            // Demo orders are persisted exactly like real orders, but never touch Razorpay.
            orderRepo.save(order);

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setTransactionId(System.currentTimeMillis());
            payment.setPayId("DEMO-" + order.getOrderId());
            payment.setAmount(totalAmount);
            payment.setProductAmount(productAmount);
            payment.setLogisticsAmount(logistics.getLogisticsCost());
            payment.setTotalAmount(totalAmount);
            payment.setPaymentMode("Demo");
            payment.setPaymentSource("DEMO");
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepo.save(payment);

            // Reuse the existing notification pipeline. No real payment email is sent.
            sendAutoEmail.SendOrderConfirmationEmail(order);

            attributes.addFlashAttribute(
                    "success",
                    "Demo order #" + order.getOrderId()
                            + " created. No Razorpay payment was made.");
            return "redirect:/Merchant/DemoOrders";

        } catch (Exception e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/Merchant/Dashboard";
        }
    }

    @GetMapping("/DemoOrders")
    @Transactional(readOnly = true)
    public String showDemoOrders(Model model, RedirectAttributes attributes) {
        Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
        if (merchant == null) {
            return "redirect:/Mlogin";
        }

        try {
            demoAccessService.requireMerchant(merchant);
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Demo access is not enabled for this account.");
            return "redirect:/Merchant/Dashboard";
        }

        List<Order> orderList =
                orderRepo.findByMerchantAndDemoOrder(merchant, true);

        Map<Long, FPOShipment> shipmentMap =
                new HashMap<>();

        for (Order order : orderList) {
            shipmentRepo.findByOrder(order)
                    .ifPresent(shipment ->
                            shipmentMap.put(order.getOrderId(), shipment));
        }

        model.addAttribute("orderList", orderList);
        model.addAttribute("shipmentMap", shipmentMap);
        model.addAttribute("demoMode", true);

        return "Merchant/myorders";
    }

    @GetMapping("/MyOrders")
    public String ShowMyOrders(
            Model model,
            RedirectAttributes attributes) {

        if (session.getAttribute(
                "loggedInMerchant") == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Session Expired ⚠️"
            );

            return "redirect:/Mlogin";
        }

        Merchant merchant =
                (Merchant) session.getAttribute(
                        "loggedInMerchant"
                );

        List<Order> orderList =
                orderRepo.findByMerchantAndDemoOrder(
                        merchant, false
                ).reversed();

        model.addAttribute(
                "orderList",
                orderList
        );

        // Shipment information for the existing
        // Merchant My Orders page.
        Map<Long, FPOShipment> shipmentMap =
                new HashMap<>();

        for (Order order : orderList) {

            shipmentRepo.findByOrder(order)
                    .ifPresent(
                            shipment ->
                                    shipmentMap.put(
                                            order.getOrderId(),
                                            shipment
                                    )
                    );
        }

        model.addAttribute(
                "shipmentMap",
                shipmentMap
        );

        return "Merchant/myorders";
    }


    @GetMapping("/CancelOrder")
    @Transactional
    public String ShowCancelOrder(
            @RequestParam("id") long orderId,
            RedirectAttributes attributes) {

        if (session.getAttribute(
                "loggedInMerchant") == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Session Expired ⚠️"
            );

            return "redirect:/Mlogin";
        }

        try {

            Order order =
                    orderRepo.findByOrderId(
                            orderId
                    );

            Payment payment =
                    paymentRepo.findByOrder(
                            order
                    );

            if (order != null
                    && payment != null
                    && (
                            "Confirmed"
                                    .equals(
                                            order.getOrderStatus()
                                    )
                            || "Pending"
                                    .equals(
                                            order.getOrderStatus()
                                    )
                    )) {

                // Demo orders never charged Razorpay and never consumed inventory.
                if (!order.isDemoOrder()) {
                    paymentService.refundPayment(
                            payment.getPayId()
                    );
                }

                if (!order.isDemoOrder() && order.getFpoProduct() != null) {

                    FPOProduct bulk =
                            order.getFpoProduct();

                    bulk.setAvailableQuantity(
                            bulk.getAvailableQuantity()
                                    + order.getQuantity()
                    );

                    bulk.setStatus(
                            "Available"
                    );

                    fpoProductRepo.save(
                            bulk
                    );


                    for (
                            OrderFarmerContribution allocation :
                            orderFarmerContributionRepo
                                    .findByOrder(order)
                    ) {

                        FPOProductContribution contribution =
                                contributionRepo
                                        .findByFpoProductAndFarmer(
                                                bulk,
                                                allocation.getFarmer()
                                        );

                        if (contribution != null) {

                            contribution.setQuantityAvailable(
                                    contribution.getQuantityAvailable()
                                            + allocation.getQuantity()
                            );

                            contributionRepo.save(
                                    contribution
                            );

                            Product sourceProduct =
                                    contribution
                                            .getSourceProduct();

                            if (sourceProduct != null) {

                                int restored =
                                        sourceProduct.getQuantity()
                                                + allocation.getQuantity();

                                sourceProduct.setQuantity(
                                        restored
                                );

                                sourceProduct.setSellingMode(
                                        "FPO"
                                );

                                sourceProduct.setStatus(
                                        "Available"
                                );

                                productRepo.save(
                                        sourceProduct
                                );
                            }

                            sendAutoEmail
                                    .SendBulkFarmerContributionEmail(
                                            order,
                                            allocation,
                                            "Cancelled"
                                    );
                        }
                    }
                }

                sendAutoEmail
                        .SendOrderCancellationEmail(
                                order,
                                payment
                        );

                order.setOrderStatus(
                        "Cancelled"
                );

                orderRepo.save(
                        order
                );
            }

            return "redirect:/Merchant/MyOrders";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Unable to cancel order: "
                            + e.getMessage()
            );

            return "redirect:/Merchant/MyOrders";
        }
    }


    @GetMapping("/ChangePassword")
    public String ShowChangePassword(
            RedirectAttributes attributes) {

        if (session.getAttribute(
                "loggedInMerchant") == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Session Expired ⚠️"
            );

            return "redirect:/Mlogin";
        }

        return "Merchant/ChangePassword";
    }


    @PostMapping("/ChangePassword")
    public String ChangePassword(
            HttpServletRequest request,
            RedirectAttributes attributes) {

        try {

            Merchant merchant =
                    (Merchant) session.getAttribute(
                            "loggedInMerchant"
                    );

            String newPassword =
                    request.getParameter(
                            "newpass"
                    );

            String confirmPassword =
                    request.getParameter(
                            "confirmpass"
                    );

            String oldPassword =
                    request.getParameter(
                            "oldpass"
                    );

            if (newPassword.equals(
                    oldPassword)) {

                attributes.addFlashAttribute(
                        "error",
                        "New password & Old Password is same, please try different."
                );

                return "redirect:/Merchant/ChangePassword";
            }

            if (!newPassword.equals(
                    confirmPassword)) {

                attributes.addFlashAttribute(
                        "error",
                        "New password and Confirm Password do not match."
                );

                return "redirect:/Merchant/ChangePassword";
            }

            if (!merchant.getPassword().equals(
                    oldPassword)) {

                attributes.addFlashAttribute(
                        "error",
                        "Invalid Old Password."
                );

                return "redirect:/Merchant/ChangePassword";
            }

            merchant.setPassword(
                    confirmPassword
            );

            merchantRepo.save(
                    merchant
            );

            session.invalidate();

            attributes.addFlashAttribute(
                    "msg",
                    "Password Successfully Changed"
            );

            return "redirect:/Mlogin";

        } catch (Exception e) {

            attributes.addFlashAttribute(
                    "error",
                    "Error : " + e.getMessage()
            );

            return "redirect:/Merchant/ChangePassword";
        }
    }


    @GetMapping("/Logout")
    public String Logout(
            RedirectAttributes attributes) {

        if (session.getAttribute(
                "loggedInMerchant") == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Session Expired ⚠️"
            );

            return "redirect:/Mlogin";
        }

        session.removeAttribute(
                "loggedInMerchant"
        );

        attributes.addFlashAttribute(
                "msg",
                "Logout Successful ✅"
        );

        return "redirect:/Mlogin";
    }
}