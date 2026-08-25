package com.swp391.techforge.controller.cart;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.Voucher;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.cart.CartService;
import com.swp391.techforge.service.order.VoucherService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private static final String VOUCHER_SESSION_KEY = "APPLIED_VOUCHER_DISCOUNT";
    private static final String VOUCHER_CODE_SESSION_KEY = "APPLIED_VOUCHER_CODE";

    private final CartService cartService;
    private final VoucherService voucherService;
    private final UserRepository userRepository;

    // 1. Màn hình Xem Giỏ hàng (View Cart)
    @GetMapping
    public String viewCart(HttpSession session, Model model, Principal principal) {
        List<CartItemDTO> cartItems = cartService.getCart(principal, session);

        // Tính tổng tiền tạm tính
        double subtotal = 0.0;
        for (CartItemDTO item : cartItems) {
            subtotal += item.getTotalPrice();
        }

        // Lấy số tiền giảm giá từ Voucher trong Session (nếu có)
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
                            Principal principal,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        cartService.addToCart(productId, productName, price, imageUrl, quantity, principal, session);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm \"" + productName + "\" vào giỏ hàng thành công!");

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
                                                      Principal principal,
                                                      HttpSession session) {

        cartService.addToCart(productId, productName, price, imageUrl, quantity, principal, session);
        int totalItemCount = cartService.getCartCount(principal, session);

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
                                                              Principal principal,
                                                              HttpSession session) {
        cartService.addMultipleToCart(items, principal, session);
        int totalItemCount = cartService.getCartCount(principal, session);

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
                                 Principal principal,
                                 HttpSession session) {

        cartService.updateQuantity(productId, quantity, principal, session);
        return "redirect:/cart";
    }

    // 4. Xóa sản phẩm khỏi giỏ hàng (Remove Item)
    @PostMapping("/remove")
    public String removeItem(@RequestParam("productId") Long productId,
                             Principal principal,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        cartService.removeFromCart(productId, principal, session);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng!");

        return "redirect:/cart";
    }

    // 5. Áp dụng Voucher giảm giá (Apply Voucher)
    @PostMapping("/apply-voucher")
    public String applyVoucher(@RequestParam("voucherCode") String voucherCode,
                               HttpSession session,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        List<CartItemDTO> cartItems = cartService.getCart(principal, session);
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
}