package com.swp391.techforge.controller.customer;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.OrderItem;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.order.OrderService;
import com.swp391.techforge.service.warranty.WarrantyTicketService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class CustomerWarrantyController {

    private final WarrantyTicketService warrantyTicketService;
    private final UserRepository userRepository;
    private final OrderService orderService;

    public CustomerWarrantyController(WarrantyTicketService warrantyTicketService, UserRepository userRepository,
            OrderService orderService) {
        this.warrantyTicketService = warrantyTicketService;
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    @GetMapping("/customer/warranty/create")
    public String form(@RequestParam Long orderItemId, Principal principal, Model model) {
        User customer = currentUser(principal);
        OrderItem orderItem = orderService.getOrderItemByIdForUser(orderItemId, customer)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong đơn hàng của bạn."));
        model.addAttribute("orderItem", orderItem);
        model.addAttribute("order", orderItem.getOrder());
        return "customer/warranty-create";
    }

    @PostMapping("/customer/warranty/create")
    public String create(@RequestParam Long orderId,
            @RequestParam Long orderItemId,
            @RequestParam String imeiSerial,
            @RequestParam(required = false) String issueDesc,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        User customer = currentUser(principal);
        try {
            warrantyTicketService.createForCustomer(orderItemId, customer, imeiSerial, issueDesc);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo phiếu bảo hành thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders/" + orderId;
    }

    private User currentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản."));
    }
}
