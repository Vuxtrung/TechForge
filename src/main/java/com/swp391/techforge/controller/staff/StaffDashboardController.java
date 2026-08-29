package com.swp391.techforge.controller.staff;

import com.swp391.techforge.service.order.OrderService;
import com.swp391.techforge.service.warranty.WarrantyTicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.Authentication;

@Controller
@RequestMapping("/staff")
public class StaffDashboardController {
    private final OrderService orderService;
    private final WarrantyTicketService warrantyTicketService;

    public StaffDashboardController(OrderService orderService, WarrantyTicketService warrantyTicketService) {
        this.orderService = orderService;
        this.warrantyTicketService = warrantyTicketService;
    }

    @GetMapping({"", "/dashboard"})
        public String dashboard(Model model, Authentication authentication) {
        boolean salesStaff = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF_SALES"));
        boolean warrantyStaff = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF_WARRANTY"));
        if (salesStaff) {
            Page<?> orders = orderService.searchForStaff(null, null, null, null, 0, 1,
                Sort.by(Sort.Direction.DESC, "orderDate"));
            model.addAttribute("orderCount", orders.getTotalElements());
        }
        if (warrantyStaff) {
            Page<?> tickets = warrantyTicketService.search(null, null, 0, 1,
                Sort.by(Sort.Direction.DESC, "createdAt"));
            model.addAttribute("warrantyCount", tickets.getTotalElements());
        }
        model.addAttribute("salesStaff", salesStaff);
        model.addAttribute("warrantyStaff", warrantyStaff);
        return "staff/dashboard";
    }
}
