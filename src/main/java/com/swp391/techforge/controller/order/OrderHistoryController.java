package com.swp391.techforge.controller.order;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.entity.*;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.order.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderHistoryController {

    private static final String CART_SESSION_KEY = "MY_CART_ITEMS";

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderHistoryController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String viewOrderHistory(@RequestParam(value = "status", required = false) String statusStr,
                                   @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   @RequestParam(value = "search", required = false) String search,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model,
                                   Principal principal) {

        OrderStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = OrderStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (Exception ignored) {
            }
        }

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        User loggedInUser = null;
        if (principal != null) {
            loggedInUser = userRepository.findByEmail(principal.getName()).orElse(null);
        }

        Page<Order> orderPage = orderService.searchForCustomer(loggedInUser, status, startDateTime, endDateTime,
            search, page, size);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("selectedStatus", statusStr);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);
        model.addAttribute("size", size > 0 && size <= 100 ? size : 10);

        return "order-history";
    }

    @GetMapping("/{id}")
    public String viewOrderDetail(@PathVariable("id") Long id, Model model, Principal principal) {
        Optional<Order> orderOpt = orderService.getOrderByIdForUser(id, currentUser(principal));
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }

        model.addAttribute("order", orderOpt.get());
        return "order-detail";
    }

    @GetMapping("/success/{id}")
    public String viewOrderSuccess(@PathVariable("id") Long id, Model model, Principal principal) {
        Optional<Order> orderOpt = orderService.getOrderByIdForUser(id, currentUser(principal));
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }

        model.addAttribute("order", orderOpt.get());
        return "order-success";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long id,
                              @RequestParam(value = "cancelReason", defaultValue = "Khách hàng đổi ý không muốn mua nữa") String cancelReason,
                              RedirectAttributes redirectAttributes,
                              Principal principal) {

        boolean success = orderService.cancelOrder(id, cancelReason, currentUser(principal));
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng #" + id + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng này!");
        }

        return "redirect:/orders/" + id;
    }

    @GetMapping("/{id}/print")
    public String printInvoice(@PathVariable("id") Long id, Model model, Principal principal) {
        Optional<Order> orderOpt = orderService.getOrderByIdForUser(id, currentUser(principal));
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }

        model.addAttribute("order", orderOpt.get());
        return "invoice-print";
    }

    @PostMapping("/{id}/reorder")
    public String reorder(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes,
                          Principal principal) {
        Optional<Order> orderOpt = orderService.getOrderByIdForUser(id, currentUser(principal));
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }

        Order order = orderOpt.get();
        @SuppressWarnings("unchecked")
        List<CartItemDTO> cart = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
        }

        for (OrderItem item : order.getOrderItems()) {
            Long productId = (item.getProduct() != null) ? item.getProduct().getProductId() : 1L;
            double price = item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0;

            boolean found = false;
            for (CartItemDTO cartItem : cart) {
                if (cartItem.getProductId().equals(productId)) {
                    cartItem.setQuantity(cartItem.getQuantity() + item.getQuantity());
                    found = true;
                    break;
                }
            }

            if (!found) {
                cart.add(new CartItemDTO(productId, item.getProductName(), "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=400", price, item.getQuantity()));
            }
        }

        session.setAttribute(CART_SESSION_KEY, cart);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm lại các sản phẩm vào giỏ hàng!");
        return "redirect:/cart";
    }

    private User currentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản."));
    }
}
