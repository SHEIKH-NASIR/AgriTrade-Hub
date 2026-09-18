package com.softpro.ATH.Controller;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.FPOProduct;
import com.softpro.ATH.Model.FPOProductContribution;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Product;
import com.softpro.ATH.Repository.FPOProductContributionRepo;
import com.softpro.ATH.Repository.FPOProductRepo;
import com.softpro.ATH.Repository.FPORepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.ProductRepo;
import com.softpro.ATH.Service.FairPriceService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/FPO/Products")
public class FPOProductController {

    @Autowired private FPOProductRepo fpoProductRepo;
    @Autowired private FPORepo fpoRepo;
    @Autowired private FormersRepo formersRepo;
    @Autowired private ProductRepo productRepo;
    @Autowired private FPOProductContributionRepo contributionRepo;
    @Autowired private FairPriceService fairPriceService;

    private FPO getLoggedInFPO(HttpSession session) {
        Object id = session.getAttribute("loggedInFPOId");
        if (id == null) return null;
        try {
            return fpoRepo.findById(Long.parseLong(id.toString())).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }


    // =========================================================
    // FIND COMMITTED MEMBER PRODUCE FOR THIS FPO
    //
    // FPO membership is the source of truth.
    // Do not require Product.fpo_id to be populated.
    // =========================================================
    private List<Product> findCommittedProductsForFpo(FPO fpo) {

        if (fpo == null
                || fpo.getMembers() == null
                || fpo.getMembers().isEmpty()) {
            return List.of();
        }

        List<Long> memberIds =
                fpo.getMembers()
                        .stream()
                        .filter(member -> member != null)
                        .map(Formers::getId)
                        .toList();

        if (memberIds.isEmpty()) {
            return List.of();
        }

        return productRepo.findCommittedProductsForFpoMembers(
                memberIds,
                "FPO"
        );
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String showProducts(HttpSession session, Model model, RedirectAttributes attributes) {
        FPO fpo = getLoggedInFPO(session);
        if (fpo == null) {
            attributes.addFlashAttribute("error", "FPO session expired. Please login again.");
            return "redirect:/FPO/Login";
        }

        List<FPOProduct> products = fpoProductRepo.findByFpo(fpo);
        List<Product> committedProducts = findCommittedProductsForFpo(fpo);

        Map<String, Integer> committedSummary = new LinkedHashMap<>();
        for (Product p : committedProducts) {
            String key = p.getProductName() + " | " + p.getCategory();
            committedSummary.merge(key, p.getQuantity(), Integer::sum);
        }

        model.addAttribute("fpo", fpo);
        model.addAttribute("products", products);
        model.addAttribute("committedProducts", committedProducts);
        model.addAttribute("committedSummary", committedSummary);
        model.addAttribute("pageTitle", "Bulk Products");
        model.addAttribute("active3", "active");
        return "FPO/products";
    }

    @GetMapping("/Edit")
    @Transactional(readOnly = true)
    public String editProduct(@RequestParam("productId") Long productId,
                              HttpSession session,
                              Model model,
                              RedirectAttributes attributes) {
        FPO fpo = getLoggedInFPO(session);
        if (fpo == null) {
            attributes.addFlashAttribute("error", "FPO session expired. Please login again.");
            return "redirect:/FPO/Login";
        }

        FPOProduct product = fpoProductRepo.findById(productId).orElse(null);
        if (product == null || product.getFpo() == null || product.getFpo().getId() != fpo.getId()) {
            attributes.addFlashAttribute("error", "Bulk product not found or unauthorized.");
            return "redirect:/FPO/Products";
        }

        List<FPOProductContribution> contributions = contributionRepo.findByFpoProduct(product);
        model.addAttribute("fpo", fpo);
        model.addAttribute("product", product);
        model.addAttribute("contributedQuantity", contributions.stream().mapToInt(FPOProductContribution::getQuantityContributed).sum());
        model.addAttribute("soldQuantity", contributions.stream().mapToInt(c -> c.getQuantityContributed() - c.getQuantityAvailable()).sum());
        model.addAttribute("active3", "active");
        return "FPO/editproduct";
    }

    @PostMapping("/Add")
    @Transactional
    public String addProduct(
            @RequestParam("sourceKey") String sourceKey,
            @RequestParam("pricePerUnit") BigDecimal pricePerUnit,
            @RequestParam("minimumBulkQuantity") int minimumBulkQuantity,
            @RequestParam(value="maxBulkQuantity", defaultValue="0") int maxBulkQuantity,
            HttpSession session,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);
        if (fpo == null) {
            attributes.addFlashAttribute("error", "FPO session expired. Please login again.");
            return "redirect:/FPO/Login";
        }

        try {
            sourceKey = sourceKey == null ? "" : sourceKey.trim();
            String[] keyParts = sourceKey.split("\\|\\|", 2);
            if (keyParts.length != 2 || keyParts[0].isBlank() || keyParts[1].isBlank()) {
                throw new IllegalArgumentException("Select a valid committed member product.");
            }
            String productName = keyParts[0].trim();
            String category = keyParts[1].trim();
            if (pricePerUnit == null || pricePerUnit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price per unit must be greater than 0.");
            }
            if (minimumBulkQuantity <= 0) {
                throw new IllegalArgumentException("Minimum bulk quantity must be greater than 0.");
            }
            if (maxBulkQuantity > 0 && maxBulkQuantity < minimumBulkQuantity) {
                throw new IllegalArgumentException("Maximum bulk quantity cannot be less than minimum bulk quantity.");
            }

            com.softpro.ATH.Dto.FairPriceResponse fairCheck = fairPriceService.checkPrice(productName, category, pricePerUnit);
            if ("ABOVE_MARKET".equals(fairCheck.getStatus())) {
                throw new IllegalArgumentException(fairCheck.getMessage() + " Reference: ₹" + fairCheck.getReferencePrice() + ", fair upper limit: ₹" + fairCheck.getUpperFairPrice() + ".");
            }

            List<Product> sourceProducts = findCommittedProductsForFpo(fpo).stream()
                    .filter(p -> productName.equalsIgnoreCase(p.getProductName()))
                    .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                    .collect(Collectors.toList());

            if (sourceProducts.isEmpty()) {
                throw new IllegalArgumentException(
                        "No member farmer has committed available produce for " + productName + " in this FPO.");
            }

            FPOProduct existing = fpoProductRepo.findByFpo(fpo).stream()
                    .filter(p -> p.getStatus() != null && !"OutOfStock".equalsIgnoreCase(p.getStatus()))
                    .filter(p -> productName.equalsIgnoreCase(p.getProductName()))
                    .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                    .findFirst().orElse(null);

            if (existing != null) {
                throw new IllegalArgumentException(
                        "An active FPO bulk product for this product already exists. Use Manage Farmers → Sync New Contributions.");
            }

            int total = sourceProducts.stream().mapToInt(Product::getQuantity).sum();
            if (minimumBulkQuantity > total) {
                throw new IllegalArgumentException(
                        "Minimum bulk quantity cannot exceed the currently committed quantity of " + total + ".");
            }

            FPOProduct product = new FPOProduct();
            product.setFpo(fpo);
            product.setProductName(productName);
            product.setCategory(category);
            product.setTotalQuantity(total);
            product.setAvailableQuantity(total);
            product.setPricePerUnit(pricePerUnit);
            product.setMinimumBulkQuantity(minimumBulkQuantity);
            product.setMaxBulkQuantity(maxBulkQuantity);
            product.setStatus("Available");
            fpoProductRepo.save(product);

            for (Product source : sourceProducts) {
                FPOProductContribution contribution = new FPOProductContribution();
                contribution.setFpoProduct(product);
                contribution.setFarmer(source.getFarmer());
                contribution.setSourceProduct(source);
                contribution.setQuantityContributed(source.getQuantity());
                contribution.setQuantityAvailable(source.getQuantity());
                contributionRepo.save(contribution);

                source.setFpoProduct(product);
                source.setSellingMode("FPO");
                source.setFpo(fpo);
                productRepo.save(source);
            }

            attributes.addFlashAttribute("success",
                    "FPO bulk product created from " + sourceProducts.size() + " farmer contribution(s), total " + total + ".");
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Unable to create FPO bulk product: " + e.getMessage());
        }

        return "redirect:/FPO/Products";
    }

    @PostMapping("/Edit")
    @Transactional
    public String updateProduct(
            @RequestParam("productId") Long productId,
            @RequestParam("productName") String productName,
            @RequestParam("category") String category,
            @RequestParam("totalQuantity") int totalQuantity,
            @RequestParam("pricePerUnit") BigDecimal pricePerUnit,
            @RequestParam("minimumBulkQuantity") int minimumBulkQuantity,
            @RequestParam(value="maxBulkQuantity", defaultValue="0") int maxBulkQuantity,
            HttpSession session,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);
        if (fpo == null) {
            attributes.addFlashAttribute("error", "FPO session expired. Please login again.");
            return "redirect:/FPO/Login";
        }

        try {
            FPOProduct product = fpoProductRepo.findById(productId).orElse(null);
            if (product == null || product.getFpo() == null || product.getFpo().getId() != fpo.getId()) {
                throw new IllegalArgumentException("Bulk product not found or unauthorized.");
            }

            productName = productName == null ? "" : productName.trim();
            category = category == null ? "" : category.trim();

            if (productName.isBlank()) {
                throw new IllegalArgumentException("Product name is required.");
            }
            if (category.isBlank()) {
                throw new IllegalArgumentException("Category is required.");
            }
            if (totalQuantity < 0) {
                throw new IllegalArgumentException("Aggregated quantity cannot be negative.");
            }
            if (pricePerUnit == null || pricePerUnit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price per unit must be greater than 0.");
            }
            if (minimumBulkQuantity <= 0) {
                throw new IllegalArgumentException("Minimum bulk quantity must be greater than 0.");
            }
            if (maxBulkQuantity > 0 && maxBulkQuantity < minimumBulkQuantity) {
                throw new IllegalArgumentException("Maximum bulk quantity cannot be less than minimum bulk quantity.");
            }

            com.softpro.ATH.Dto.FairPriceResponse fairCheck = fairPriceService.checkPrice(productName, category, pricePerUnit);
            if ("ABOVE_MARKET".equals(fairCheck.getStatus())) {
                throw new IllegalArgumentException(fairCheck.getMessage() + " Reference: ₹" + fairCheck.getReferencePrice() + ", fair upper limit: ₹" + fairCheck.getUpperFairPrice() + ".");
            }

            // Quantity already sold cannot be made unavailable by reducing total quantity.
            int alreadySold = contributionRepo.findByFpoProduct(product)
                    .stream()
                    .mapToInt(c -> c.getQuantityContributed() - c.getQuantityAvailable())
                    .sum();

            if (totalQuantity < alreadySold) {
                throw new IllegalArgumentException(
                        "Aggregated quantity cannot be less than already sold quantity (" + alreadySold + ").");
            }

            if (minimumBulkQuantity > totalQuantity) {
                throw new IllegalArgumentException(
                        "Minimum bulk quantity cannot exceed the aggregated quantity.");
            }

            // Keep available quantity consistent with the edited total and already-sold quantity.
            int newAvailableQuantity = totalQuantity - alreadySold;

            product.setProductName(productName);
            product.setCategory(category);
            product.setTotalQuantity(totalQuantity);
            product.setAvailableQuantity(newAvailableQuantity);
            product.setPricePerUnit(pricePerUnit);
            product.setMinimumBulkQuantity(minimumBulkQuantity);
            product.setMaxBulkQuantity(maxBulkQuantity);
            product.setStatus(newAvailableQuantity > 0 ? "Available" : "OutOfStock");

            fpoProductRepo.save(product);

            attributes.addFlashAttribute("success",
                    "FPO bulk product updated successfully.");
            return "redirect:/FPO/Products";
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Unable to update bulk product: " + e.getMessage());
            return "redirect:/FPO/Products/Edit?productId=" + productId;
        }
    }

    @GetMapping("/Contributions")
    @Transactional(readOnly = true)
    public String contributions(
            @RequestParam("productId") Long productId,
            HttpSession session,
            Model model,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);
        if (fpo == null) {
            attributes.addFlashAttribute("error", "FPO session expired. Please login again.");
            return "redirect:/FPO/Login";
        }

        FPOProduct product = fpoProductRepo.findById(productId).orElse(null);
        if (product == null || product.getFpo() == null || product.getFpo().getId() != fpo.getId()) {
            attributes.addFlashAttribute("error", "Bulk product not found or unauthorized.");
            return "redirect:/FPO/Products";
        }

        List<FPOProductContribution> contributions = contributionRepo.findByFpoProduct(product);
        List<Product> newCommittedProducts = findCommittedProductsForFpo(fpo).stream()
                .filter(p -> product.getProductName().equalsIgnoreCase(p.getProductName()))
                .filter(p -> product.getCategory().equalsIgnoreCase(p.getCategory()))
                .collect(Collectors.toList());

        int allocated = contributions.stream().mapToInt(FPOProductContribution::getQuantityContributed).sum();
        int available = contributions.stream().mapToInt(FPOProductContribution::getQuantityAvailable).sum();
        int sold = allocated - available;

        model.addAttribute("fpo", fpo);
        model.addAttribute("product", product);
        model.addAttribute("contributions", contributions);
        model.addAttribute("newCommittedProducts", newCommittedProducts);
        model.addAttribute("allocatedQuantity", allocated);
        model.addAttribute("availableContributionQuantity", available);
        model.addAttribute("soldQuantity", sold);
        model.addAttribute("remainingToAllocate", Math.max(0, product.getTotalQuantity() - allocated));
        model.addAttribute("active3", "active");
        return "FPO/contributions";
    }

    @PostMapping("/Contributions/Sync")
    @Transactional
    public String syncContributions(
            @RequestParam("productId") Long productId,
            HttpSession session,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);
        if (fpo == null) {
            attributes.addFlashAttribute("error", "FPO session expired. Please login again.");
            return "redirect:/FPO/Login";
        }

        try {
            FPOProduct product = fpoProductRepo.findById(productId).orElse(null);
            if (product == null || product.getFpo() == null || product.getFpo().getId() != fpo.getId()) {
                throw new IllegalArgumentException("Bulk product not found or unauthorized.");
            }

            List<Product> sources = findCommittedProductsForFpo(fpo).stream()
                    .filter(p -> product.getProductName().equalsIgnoreCase(p.getProductName()))
                    .filter(p -> product.getCategory().equalsIgnoreCase(p.getCategory()))
                    .collect(Collectors.toList());

            int added = 0;
            for (Product source : sources) {
                if (source.getFpoProduct() != null) continue;

                FPOProductContribution contribution = new FPOProductContribution();
                contribution.setFpoProduct(product);
                contribution.setFarmer(source.getFarmer());
                contribution.setSourceProduct(source);
                contribution.setQuantityContributed(source.getQuantity());
                contribution.setQuantityAvailable(source.getQuantity());
                contributionRepo.save(contribution);

                source.setFpoProduct(product);
                productRepo.save(source);
                added += source.getQuantity();
            }

            recalculateAggregate(product);
            fpoProductRepo.save(product);

            attributes.addFlashAttribute("success",
                    added > 0 ? "New farmer contributions synchronized into this bulk product." : "No new committed farmer products were waiting for synchronization.");
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Unable to sync contributions: " + e.getMessage());
        }

        return "redirect:/FPO/Products/Contributions?productId=" + productId;
    }

    @PostMapping("/Contributions/Remove")
    @Transactional
    public String removeContribution(
            @RequestParam("contributionId") Long contributionId,
            HttpSession session,
            RedirectAttributes attributes) {

        FPO fpo = getLoggedInFPO(session);
        if (fpo == null) {
            attributes.addFlashAttribute("error", "FPO session expired. Please login again.");
            return "redirect:/FPO/Login";
        }

        try {
            FPOProductContribution contribution = contributionRepo.findById(contributionId).orElse(null);
            if (contribution == null || contribution.getFpoProduct() == null
                    || contribution.getFpoProduct().getFpo().getId() != fpo.getId()) {
                throw new IllegalArgumentException("Contribution not found or unauthorized.");
            }

            if (contribution.getQuantityAvailable() < contribution.getQuantityContributed()) {
                throw new IllegalArgumentException("This contribution already has sold quantity and cannot be removed.");
            }

            Product source = contribution.getSourceProduct();
            if (source != null) {
                source.setFpoProduct(null);
                source.setSellingMode("FPO");
                productRepo.save(source);
            }

            FPOProduct product = contribution.getFpoProduct();
            contributionRepo.delete(contribution);
            recalculateAggregate(product);
            fpoProductRepo.save(product);

            attributes.addFlashAttribute("success", "Farmer contribution removed from this bulk product.");
            return "redirect:/FPO/Products/Contributions?productId=" + product.getId();
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Unable to remove contribution: " + e.getMessage());
            return "redirect:/FPO/Products";
        }
    }

    private void recalculateAggregate(FPOProduct product) {
        List<FPOProductContribution> contributions = contributionRepo.findByFpoProduct(product);
        int total = contributions.stream().mapToInt(FPOProductContribution::getQuantityContributed).sum();
        int available = contributions.stream().mapToInt(FPOProductContribution::getQuantityAvailable).sum();
        product.setTotalQuantity(total);
        product.setAvailableQuantity(available);
        product.setStatus(available > 0 ? "Available" : "OutOfStock");
    }

    @GetMapping("/fair-price")
    @ResponseBody
    public com.softpro.ATH.Dto.FairPriceResponse fairPrice(
            @RequestParam String productName,
            @RequestParam(required=false) String category,
            @RequestParam BigDecimal price) {
        return fairPriceService.checkPrice(productName, category, price);
    }

}