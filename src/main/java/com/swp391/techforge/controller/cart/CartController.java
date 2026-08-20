package com.swp391.techforge.controller.cart;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.repository.product.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final String CART_SESSION_KEY = "MY_CART_ITEMS";
    private static final String VOUCHER_SESSION_KEY = "APPLIED_VOUCHER_DISCOUNT";

    private final ProductRepository productRepository;

    public CartController(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
    @PostMapping("/apply-voucher")
    public String applyVoucher(@RequestParam("voucherCode") String voucherCode,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            String code = voucherCode.trim().toUpperCase();
            
            // Giả lập kiểm tra Voucher cơ bản
            if (code.equals("TECHFORGE10")) {
                // Giảm 10% tổng hóa đơn (ví dụ giả lập giảm 500,000đ)
                session.setAttribute(VOUCHER_SESSION_KEY, 500000.0);
                session.setAttribute("APPLIED_VOUCHER_CODE", code);
                redirectAttributes.addFlashAttribute("voucherSuccessMessage", "Áp dụng mã TECHFORGE10 giảm 500.000đ thành công!");
            } else if (code.equals("DISCOUNT50K")) {
                session.setAttribute(VOUCHER_SESSION_KEY, 50000.0);
                session.setAttribute("APPLIED_VOUCHER_CODE", code);
                redirectAttributes.addFlashAttribute("voucherSuccessMessage", "Áp dụng mã DISCOUNT50K giảm 50.000đ thành công!");
            } else {
                redirectAttributes.addFlashAttribute("voucherErrorMessage", "Mã giảm giá không hợp lệ hoặc đã hết hạn!");
            }
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
