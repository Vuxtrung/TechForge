package com.swp391.techforge.controller.order;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.entity.*;
import com.swp391.techforge.service.order.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String viewOrderHistory(@RequestParam(value = "status", required = false) String statusStr,
                                   @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   @RequestParam(value = "search", required = false) String search,
                                   Model model) {

        OrderStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = OrderStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (Exception ignored) {
            }
        }

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        List<Order> orders = orderService.getCustomerOrders(null, status, startDateTime, endDateTime, search);

        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", statusStr);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);

        return "order-history";
    }

    @GetMapping("/{id}")
    public String viewOrderDetail(@PathVariable("id") Long id, Model model) {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }

        model.addAttribute("order", orderOpt.get());
        return "order-detail";
    }

    @GetMapping("/success/{id}")
    public String viewOrderSuccess(@PathVariable("id") Long id, Model model) {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }

        model.addAttribute("order", orderOpt.get());
        return "order-success";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long id,
                              @RequestParam(value = "cancelReason", defaultValue = "Khách hàng đổi ý không muốn mua nữa") String cancelReason,
                              RedirectAttributes redirectAttributes) {

        boolean success = orderService.cancelOrder(id, cancelReason, null);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng #" + id + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng này!");
        }

        return "redirect:/orders/" + id;
    }

    @GetMapping("/{id}/print")
    public String printInvoice(@PathVariable("id") Long id, Model model) {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }

        model.addAttribute("order", orderOpt.get());
        return "invoice-print";
    }

    @PostMapping("/{id}/reorder")
    public String reorder(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<Order> orderOpt = orderService.getOrderById(id);
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
}
