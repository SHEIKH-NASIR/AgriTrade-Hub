package com.softpro.ATH.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.softpro.ATH.Model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.API.PaymentService;
import com.softpro.ATH.API.SendAutoEmail;
import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.FPOProduct;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Payment;
import com.softpro.ATH.Model.OrderFarmerContribution;
import com.softpro.ATH.Repository.FPOProductRepo;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.PaymentRepo;
import com.softpro.ATH.Repository.FPOProductContributionRepo;
import com.softpro.ATH.Repository.OrderFarmerContributionRepo;
import com.softpro.ATH.Repository.ProductRepo;

import com.softpro.ATH.Service.DemoAccessService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping({"/FPO", "/fpo"})
public class FPOController {

    @Autowired
    private FPORepo fpoRepo;

    @Autowired
    private FormersRepo formersRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private FPOProductRepo fpoProductRepo;

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SendAutoEmail sendAutoEmail;

    @Autowired
    private FPOProductContributionRepo contributionRepo;

    @Autowired
    private OrderFarmerContributionRepo orderFarmerContributionRepo;

    @Autowired
    private DemoAccessService demoAccessService;

    private FPO getLoggedInFPO(HttpSession session) {
        Object fpoIdObject = session.getAttribute("loggedInFPOId");

        if (fpoIdObject == null) {
            return null;
        }

        try {
            long fpoId = Long.parseLong(fpoIdObject.toString());
            return fpoRepo.findById(fpoId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================
    // GLOBAL FPO MODEL ATTRIBUTE
    // =========================================================
    // FPO/fpobase is used as the layout for several FPO pages
    // and directly accesses ${fpo.name}. Supplying fpo globally
    // prevents Thymeleaf from receiving a null fpo when a page
    // such as DemoOrders does not explicitly add it.
    @org.springframework.web.bind.annotation.ModelAttribute
    public void addLoggedInFPOToModel(
            HttpSession session,
            Model model) {

        if (!model.containsAttribute("fpo")) {
            FPO fpo = getLoggedInFPO(session);

            if (fpo != null) {
                model.addAttribute("fpo", fpo);
            }
        }
    }

    @GetMapping({
            "/Login",
            "/login"
    })
    public String showFPOLogin(Model model) {
        if (!model.containsAttribute("fpo")) {
            model.addAttribute("fpo", new FPO());
        }

        return "FPO/login";
    }

    @PostMapping({
            "/Login",
            "/login"
    })
    public String fpoLogin(
            @ModelAttribute FPO loginFPO,
            HttpSession session,
            RedirectAttributes attributes) {

        try {
            FPO fpo = fpoRepo.findByEmail(loginFPO.getEmail()).orElse(null);

            if (fpo == null) {
                attributes.addFlashAttribute(
                        "error",
                        "Invalid email or password."
                );
                return "redirect:/FPO/Login";
            }

            if (loginFPO.getPassword() == null
                    || !loginFPO.getPassword().equals(fpo.getPassword())) {

                attributes.addFlashAttribute(
                        "error",
                        "Invalid email or password."
                );
                return "redirect:/FPO/Login";
            }

            if ("Pending".equalsIgnoreCase(fpo.getStatus())) {
                attributes.addFlashAttribute(
                        "error",
                        "Your FPO account is pending admin approval."
                );
                return "redirect:/FPO/Login";
            }

            if (!"Verified".equalsIgnoreCase(fpo.getStatus())) {
                attributes.addFlashAttribute(
                        "error",
                        "Your FPO account is disabled. Please contact administrator."
                );
                return "redirect:/FPO/Login";
            }

            session.setAttribute("loggedInFPOId", fpo.getId());
            session.setAttribute("loggedInFPO", fpo);

            return "redirect:/FPO/Dashboard";

        } catch (Exception e) {
            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to login. Please try again."
            );

            return "redirect:/FPO/Login";
        }
    }

    @GetMapping({
            "/Dashboard",
            "/dashboard"
    })
    @Transactional(readOnly = true)
    public String dashboard(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {
            session.removeAttribute("loggedInFPOId");
            session.removeAttribute("loggedInFPO");

            attributes.addFlashAttribute(
                    "error",
                    "Session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }

        session.setAttribute("loggedInFPO", fpo);

        int memberCount = fpo.getMembers().size();

        long totalProducts = 0L;

        for (Formers farmer : fpo.getMembers()) {
            Object productCountObj = productRepo.countByFarmer(farmer);

            if (productCountObj != null) {
                totalProducts += ((Number) productCountObj).longValue();
            }
        }

        long totalOrders =
                orderRepo.countByFpoAndDemoOrder(fpo, false);

        long pendingOrderCount =
                orderRepo.countByFpoAndDemoOrderAndOrderStatus(
                        fpo,
                        false,
                        "Pending"
                );

        model.addAttribute("fpo", fpo);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrderCount", pendingOrderCount);
        model.addAttribute("active1", "active");

        return "FPO/dashboard";
    }

    @GetMapping("/DemoOrders")
    @Transactional(readOnly = true)
    public String demoOrders(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {
            return "redirect:/FPO/Login";
        }

        try {
            demoAccessService.requireFpo(fpo);
        } catch (Exception e) {
            attributes.addFlashAttribute(
                    "error",
                    "Demo access is not enabled for this FPO."
            );
            return "redirect:/FPO/Dashboard";
        }

        List<Order> orders =
                orderRepo.findByFpoAndDemoOrder(fpo, true);

        Map<Long, List<OrderFarmerContribution>> contributionMap =
                new HashMap<>();

        for (Order order : orders) {
            contributionMap.put(
                    order.getOrderId(),
                    orderFarmerContributionRepo.findByOrder(order)
            );
        }

        // IMPORTANT:
        // FPO/orders.html uses the shared FPO/fpobase layout,
        // which requires the "fpo" model attribute.
        model.addAttribute("fpo", fpo);

        model.addAttribute("orders", orders);
        model.addAttribute("orderContributions", contributionMap);

        model.addAttribute(
                "pendingOrderCount",
                orders.stream()
                        .filter(o ->
                                "Pending".equalsIgnoreCase(
                                        o.getOrderStatus()
                                ))
                        .count()
        );

        model.addAttribute("demoMode", true);
        model.addAttribute("active4", "active");

        return "FPO/orders";
    }

    @GetMapping({
            "/Orders",
            "/orders"
    })
    @Transactional(readOnly = true)
    public String orders(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }

        session.setAttribute("loggedInFPO", fpo);

        List<Order> orders =
                orderRepo.findByFpoAndDemoOrder(fpo, false);

        long pendingOrderCount =
                orderRepo.countByFpoAndDemoOrderAndOrderStatus(
                        fpo,
                        false,
                        "Pending"
                );

        model.addAttribute("fpo", fpo);

        Map<Long, List<OrderFarmerContribution>> contributionMap =
                new HashMap<>();

        for (Order order : orders) {
            contributionMap.put(
                    order.getOrderId(),
                    orderFarmerContributionRepo.findByOrder(order)
            );
        }

        model.addAttribute("orders", orders);
        model.addAttribute("orderContributions", contributionMap);
        model.addAttribute("pendingOrderCount", pendingOrderCount);
        model.addAttribute("active4", "active");

        return "FPO/orders";
    }

    @PostMapping({
            "/Orders/Update",
            "/orders/update"
    })
    @Transactional
    public String updateOrderStatus(
            @RequestParam("orderId") Long orderId,
            @RequestParam("newStatus") String newStatus,
            HttpSession session,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }

        boolean demoOrder = false;

        try {
            Order order = orderRepo.findByOrderId(orderId);

            if (order == null) {
                attributes.addFlashAttribute(
                        "error",
                        "Order not found."
                );

                return "redirect:/FPO/Orders";
            }

            if (order.getFpo() == null
                    || order.getFpo().getId() != fpo.getId()) {

                attributes.addFlashAttribute(
                        "error",
                        "You are not authorized to update this order."
                );

                return "redirect:/FPO/Orders";
            }

            demoOrder = order.isDemoOrder();

            if (demoOrder) {
                demoAccessService.requireFpo(fpo);
            }

            String status =
                    newStatus == null
                            ? ""
                            : newStatus.trim();

            if ("Confirmed".equalsIgnoreCase(status)) {

                if (!"Pending".equalsIgnoreCase(
                        order.getOrderStatus())) {

                    attributes.addFlashAttribute(
                            "error",
                            "Only pending orders can be confirmed."
                    );

                    return demoOrder
                            ? "redirect:/FPO/DemoOrders"
                            : "redirect:/FPO/Orders";
                }

                order.setOrderStatus("Confirmed");
                orderRepo.save(order);

                for (OrderFarmerContribution allocation :
                        orderFarmerContributionRepo.findByOrder(order)) {

                    sendAutoEmail.SendBulkFarmerContributionEmail(
                            order,
                            allocation,
                            "Confirmed"
                    );
                }

                sendAutoEmail.SendBulkOrderStatusEmail(
                        order,
                        "Confirmed"
                );

                attributes.addFlashAttribute(
                        "success",
                        "Order #" + order.getOrderId()
                                + " confirmed successfully. Merchant has been notified."
                );

                return demoOrder
                        ? "redirect:/FPO/DemoOrders"
                        : "redirect:/FPO/Orders";
            }

            if ("Cancelled".equalsIgnoreCase(status)) {

                if (!"Pending".equalsIgnoreCase(
                        order.getOrderStatus())) {

                    attributes.addFlashAttribute(
                            "error",
                            "Only pending orders can be cancelled from the FPO dashboard."
                    );

                    return demoOrder
                            ? "redirect:/FPO/DemoOrders"
                            : "redirect:/FPO/Orders";
                }

                Payment payment =
                        paymentRepo.findByOrder(order);

                if (!order.isDemoOrder()
                        && payment != null
                        && payment.getPayId() != null
                        && !payment.getPayId().isBlank()) {

                    paymentService.refundPayment(
                            payment.getPayId()
                    );
                }

                if (!order.isDemoOrder()
                        && order.getFpoProduct() != null) {

                    FPOProduct product =
                            order.getFpoProduct();

                    int restoredQuantity =
                            product.getAvailableQuantity()
                                    + order.getQuantity();

                    product.setAvailableQuantity(
                            restoredQuantity
                    );

                    if (restoredQuantity > 0) {
                        product.setStatus("Available");
                    }

                    fpoProductRepo.save(product);
                }

                for (OrderFarmerContribution allocation :
                        orderFarmerContributionRepo.findByOrder(order)) {

                    var contribution =
                            contributionRepo.findByFpoProductAndFarmer(
                                    order.getFpoProduct(),
                                    allocation.getFarmer()
                            );

                    if (contribution != null) {

                        contribution.setQuantityAvailable(
                                contribution.getQuantityAvailable()
                                        + allocation.getQuantity()
                        );

                        contributionRepo.save(contribution);

                        Product sourceProduct =
                                contribution.getSourceProduct();

                        if (sourceProduct != null) {

                            int restored =
                                    sourceProduct.getQuantity()
                                            + allocation.getQuantity();

                            sourceProduct.setQuantity(restored);
                            sourceProduct.setSellingMode("FPO");
                            sourceProduct.setStatus("Available");

                            productRepo.save(sourceProduct);
                        }
                    }
                }

                order.setOrderStatus("Cancelled");
                orderRepo.save(order);

                for (OrderFarmerContribution allocation :
                        orderFarmerContributionRepo.findByOrder(order)) {

                    sendAutoEmail.SendBulkFarmerContributionEmail(
                            order,
                            allocation,
                            "Cancelled"
                    );
                }

                sendAutoEmail.SendBulkOrderStatusEmail(
                        order,
                        "Cancelled"
                );

                attributes.addFlashAttribute(
                        "success",
                        "Order #" + order.getOrderId()
                                + " cancelled and refund initiated. Merchant has been notified."
                );

                return demoOrder
                        ? "redirect:/FPO/DemoOrders"
                        : "redirect:/FPO/Orders";
            }

            if ("Delivered".equalsIgnoreCase(status)) {

                if (!"Confirmed".equalsIgnoreCase(
                        order.getOrderStatus())) {

                    attributes.addFlashAttribute(
                            "error",
                            "Only confirmed orders can be marked as delivered."
                    );

                    return demoOrder
                            ? "redirect:/FPO/DemoOrders"
                            : "redirect:/FPO/Orders";
                }

                order.setOrderStatus("Delivered");
                order.setDeliveredDate(LocalDateTime.now());

                orderRepo.save(order);

                for (OrderFarmerContribution allocation :
                        orderFarmerContributionRepo.findByOrder(order)) {

                    sendAutoEmail.SendBulkFarmerContributionEmail(
                            order,
                            allocation,
                            "Delivered"
                    );
                }

                sendAutoEmail.SendBulkOrderStatusEmail(
                        order,
                        "Delivered"
                );

                attributes.addFlashAttribute(
                        "success",
                        "Order #" + order.getOrderId()
                                + " marked as delivered. Merchant has been notified."
                );

                return demoOrder
                        ? "redirect:/FPO/DemoOrders"
                        : "redirect:/FPO/Orders";
            }

            attributes.addFlashAttribute(
                    "error",
                    "Invalid order status."
            );

        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to update order: "
                            + e.getMessage()
            );
        }

        return demoOrder
                ? "redirect:/FPO/DemoOrders"
                : "redirect:/FPO/Orders";
    }

    @GetMapping({
            "/Members",
            "/members"
    })
    @Transactional(readOnly = true)
    public String members(
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }

        session.setAttribute("loggedInFPO", fpo);

        List<Formers> members = fpo.getMembers();

        model.addAttribute("fpo", fpo);

        model.addAttribute(
                "farmers",
                formersRepo.findAllByStatus("Verified")
        );

        model.addAttribute("members", members);
        model.addAttribute("active2", "active");

        return "FPO/members";
    }

    @PostMapping({
            "/Members/Add",
            "/members/add",
            "/addmember"
    })
    @Transactional
    public String addMember(
            @RequestParam("farmerId") long farmerId,
            HttpSession session,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }

        try {
            Formers farmer =
                    formersRepo.findById(farmerId).orElse(null);

            if (farmer == null) {
                attributes.addFlashAttribute(
                        "error",
                        "Farmer not found."
                );

                return "redirect:/FPO/Members";
            }

            if (!"Verified".equalsIgnoreCase(
                    farmer.getStatus())) {

                attributes.addFlashAttribute(
                        "error",
                        "Only verified farmers can be added."
                );

                return "redirect:/FPO/Members";
            }

            boolean alreadyMember =
                    fpo.getMembers()
                            .stream()
                            .anyMatch(
                                    member ->
                                            member.getId()
                                                    == farmer.getId()
                            );

            if (alreadyMember) {
                attributes.addFlashAttribute(
                        "error",
                        "Farmer is already an FPO member."
                );

                return "redirect:/FPO/Members";
            }

            fpo.getMembers().add(farmer);
            fpoRepo.save(fpo);

            sendAutoEmail.SendFPOFarmerAddedEmail(fpo, farmer);

            session.setAttribute("loggedInFPO", fpo);

            attributes.addFlashAttribute(
                    "success",
                    "Farmer added successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to add farmer: "
                            + e.getMessage()
            );
        }

        return "redirect:/FPO/Members";
    }

    @PostMapping({
            "/Members/Remove",
            "/members/remove"
    })
    @Transactional
    public String removeMember(
            @RequestParam("farmerId") long farmerId,
            HttpSession session,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);

        if (fpo == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Session expired. Please login again."
            );

            return "redirect:/FPO/Login";
        }

        try {
            boolean removed =
                    fpo.getMembers()
                            .removeIf(
                                    member ->
                                            member.getId()
                                                    == farmerId
                            );

            if (removed) {

                fpoRepo.save(fpo);

                session.setAttribute(
                        "loggedInFPO",
                        fpo
                );

                attributes.addFlashAttribute(
                        "success",
                        "Farmer removed from FPO."
                );

            } else {

                attributes.addFlashAttribute(
                        "error",
                        "Farmer is not an FPO member."
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            attributes.addFlashAttribute(
                    "error",
                    "Unable to remove farmer: "
                            + e.getMessage()
            );
        }

        return "redirect:/FPO/Members";
    }

    @GetMapping({
            "/Logout",
            "/logout"
    })
    public String logout(
            HttpSession session,
            RedirectAttributes attributes) {

        session.removeAttribute("loggedInFPOId");
        session.removeAttribute("loggedInFPO");

        attributes.addFlashAttribute(
                "success",
                "Successfully logged out."
        );

        return "redirect:/FPO/Login";
    }
}
