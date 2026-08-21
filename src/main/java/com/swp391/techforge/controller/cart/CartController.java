package com.swp391.techforge.controller.cart;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.Voucher;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import com.swp391.techforge.service.order.VoucherService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final String CART_SESSION_KEY = "MY_CART_ITEMS";
    private static final String VOUCHER_SESSION_KEY = "APPLIED_VOUCHER_DISCOUNT";
    private static final String VOUCHER_CODE_SESSION_KEY = "APPLIED_VOUCHER_CODE";

    private final ProductRepository productRepository;
    private final VoucherService voucherService;
    private final UserRepository userRepository;

    public CartController(ProductRepository productRepository,
                           VoucherService voucherService,
                           UserRepository userRepository) {
        this.productRepository = productRepository;
        this.voucherService = voucherService;
        this.userRepository = userRepository;
    }

    // 1. Màn hình Xem Giỏ hàng (View Cart)
    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        List<CartItemDTO> cartItems = getCartFromSession(session);

        // Đọc tồn kho thực tế từ DB cho từng sản phẩm
        for (CartItemDTO item : cartItems) {
            if (item.getProductId() != null) {
                productRepository.findById(item.getProductId())
                        .ifPresent(p -> item.setStockQuantity(p.getStockQuantity()));
            }
        }

        // Tính tổng tiền tạm tính
        double subtotal = 0.0;
        for (CartItemDTO item : cartItems) {
            subtotal += item.getTotalPrice();
        }

        // Lấy số tiền giảm giá từ Voucher & Điểm thưởng trong Session (nếu có)
        Double voucherDiscount = (Double) session.getAttribute(VOUCHER_SESSION_KEY);
        if (voucherDiscount == null) voucherDiscount = 0.0;

        // Tính tổng tiền thanh toán cuối cùng
        double grandTotal = subtotal - voucherDiscount;
        if (grandTotal < 0) grandTotal = 0.0;

        // Đưa dữ liệu sang file HTML Thymeleaf
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("voucherDiscount", voucherDiscount);
        model.addAttribute("grandTotal", grandTotal);

        return "cart";
    }

    // 2. Thêm sản phẩm vào giỏ hàng (Add to Cart)
    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(value = "productName", defaultValue = "Sản phẩm TechForge") String productName,
                            @RequestParam(value = "price", defaultValue = "1000000") Double price,
                            @RequestParam(value = "imageUrl", defaultValue = "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=400") String imageUrl,
                            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                            @RequestHeader(value = "Referer", required = false) String referer,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        List<CartItemDTO> cartItems = getCartFromSession(session);
        boolean found = false;

        // Kiểm tra nếu sản phẩm đã có trong giỏ thì tăng số lượng
        for (CartItemDTO item : cartItems) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                found = true;
                break;
            }
        }

        // Nếu chưa có thì thêm mới vào danh sách
        if (!found) {
            cartItems.add(new CartItemDTO(productId, productName, imageUrl, price, quantity));
        }

        session.setAttribute(CART_SESSION_KEY, cartItems);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm \"" + productName + "\" vào giỏ hàng thành công!");

        // Giữ khách hàng ở nguyên trang hiện tại (Trang chủ / Danh sách SP) để mua thêm món khác
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }

        return "redirect:/cart";
    }

    @PostMapping("/api/add")
    @ResponseBody
    public java.util.Map<String, Object> addToCartApi(@RequestParam("productId") Long productId,
                                                      @RequestParam(value = "productName", defaultValue = "Sản phẩm TechForge") String productName,
                                                      @RequestParam(value = "price", defaultValue = "1000000") Double price,
                                                      @RequestParam(value = "imageUrl", defaultValue = "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=400") String imageUrl,
                                                      @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                                      HttpSession session) {

        List<CartItemDTO> cartItems = getCartFromSession(session);
        boolean found = false;

        for (CartItemDTO item : cartItems) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                found = true;
                break;
            }
        }

        if (!found) {
            cartItems.add(new CartItemDTO(productId, productName, imageUrl, price, quantity));
        }

        session.setAttribute(CART_SESSION_KEY, cartItems);

        // Tính tổng số lượng tất cả các sản phẩm trong giỏ
        int totalItemCount = 0;
        for (CartItemDTO item : cartItems) {
            totalItemCount += item.getQuantity();
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("cartSize", totalItemCount);
        response.put("message", "Đã thêm \"" + productName + "\" vào giỏ hàng!");

        return response;
    }

    /**
     * API Thêm hàng loạt linh kiện PC vào giỏ hàng trong một lượt
     */
    @PostMapping("/api/add-multiple")
    @ResponseBody
    public java.util.Map<String, Object> addToCartMultipleApi(@RequestBody List<CartItemDTO> items,
                                                              HttpSession session) {
        List<CartItemDTO> cartItems = getCartFromSession(session);

        if (items != null) {
            for (CartItemDTO newItem : items) {
                if (newItem.getProductId() == null) continue;

                boolean found = false;
                for (CartItemDTO existing : cartItems) {
                    if (existing.getProductId() != null && existing.getProductId().equals(newItem.getProductId())) {
                        existing.setQuantity(existing.getQuantity() + (newItem.getQuantity() != null ? newItem.getQuantity() : 1));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    productRepository.findById(newItem.getProductId()).ifPresent(p -> {
                        if (newItem.getProductName() == null || newItem.getProductName().isEmpty()) {
                            newItem.setProductName(p.getName());
                        }
                        if (newItem.getPrice() == null) {
                            newItem.setPrice(p.getBasePrice() != null ? p.getBasePrice().doubleValue() : 0.0);
                        }
                        if (newItem.getImageUrl() == null || newItem.getImageUrl().isEmpty()) {
                            newItem.setImageUrl(p.getPrimaryImageUrl() != null ? p.getPrimaryImageUrl() : "");
                        }
                    });
                    if (newItem.getQuantity() == null || newItem.getQuantity() <= 0) {
                        newItem.setQuantity(1);
                    }
                    cartItems.add(newItem);
                }
            }
        }

        session.setAttribute(CART_SESSION_KEY, cartItems);

        int totalItemCount = 0;
        for (CartItemDTO item : cartItems) {
            totalItemCount += item.getQuantity();
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("cartSize", totalItemCount);
        response.put("message", "Đã thêm toàn bộ linh kiện vào giỏ hàng!");
        return response;
    }

    // 3. Cập nhật số lượng sản phẩm (Update Quantity)
    @PostMapping("/update")
    public String updateQuantity(@RequestParam("productId") Long productId,
                                 @RequestParam("quantity") Integer quantity,
                                 HttpSession session) {

        List<CartItemDTO> cartItems = getCartFromSession(session);

        for (CartItemDTO item : cartItems) {
            if (item.getProductId().equals(productId)) {
                if (quantity > 0) {
                    item.setQuantity(quantity);
                }
                break;
            }
        }

        session.setAttribute(CART_SESSION_KEY, cartItems);
        return "redirect:/cart";
    }

    // 4. Xóa sản phẩm khỏi giỏ hàng (Remove Item)
    @PostMapping("/remove")
    public String removeItem(@RequestParam("productId") Long productId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        List<CartItemDTO> cartItems = getCartFromSession(session);
        cartItems.removeIf(item -> item.getProductId().equals(productId));

        session.setAttribute(CART_SESSION_KEY, cartItems);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng!");

        return "redirect:/cart";
    }

    // 5. Áp dụng Voucher giảm giá (Apply Voucher)
    // Dùng chung VoucherService với CheckoutController — voucher được kiểm tra
    // thật với DB (BR-V02..BR-V06) thay vì so khớp chuỗi cứng như trước đây.
    @PostMapping("/apply-voucher")
    public String applyVoucher(@RequestParam("voucherCode") String voucherCode,
                               HttpSession session,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        List<CartItemDTO> cartItems = getCartFromSession(session);
        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("voucherErrorMessage", "Giỏ hàng của bạn đang trống!");
            return "redirect:/cart";
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemDTO item : cartItems) {
            subtotal = subtotal.add(BigDecimal.valueOf(item.getTotalPrice()));
        }

        User user = null;
        if (principal != null) {
            user = userRepository.findByEmail(principal.getName()).orElse(null);
        }

        try {
            Voucher voucher = voucherService.validateForCheckout(voucherCode, subtotal, user);
            if (voucher == null) {
                session.removeAttribute(VOUCHER_SESSION_KEY);
                session.removeAttribute(VOUCHER_CODE_SESSION_KEY);
            } else {
                BigDecimal discount = voucherService.calculateDiscount(voucher, subtotal);
                session.setAttribute(VOUCHER_SESSION_KEY, discount.doubleValue());
                session.setAttribute(VOUCHER_CODE_SESSION_KEY, voucher.getCode());
                redirectAttributes.addFlashAttribute("voucherSuccessMessage",
                        "Áp dụng mã \"" + voucher.getCode() + "\" thành công!");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            session.removeAttribute(VOUCHER_SESSION_KEY);
            session.removeAttribute(VOUCHER_CODE_SESSION_KEY);
            redirectAttributes.addFlashAttribute("voucherErrorMessage", ex.getMessage());
        }

        return "redirect:/cart";
    }

    // Hàm tiện ích lấy giỏ hàng từ Session
    @SuppressWarnings("unchecked")
    private List<CartItemDTO> getCartFromSession(HttpSession session) {
        List<CartItemDTO> cart = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
        }
        return cart;
    }
}