package com.swp391.techforge.controller.customer;

import com.swp391.techforge.entity.WarrantyTicket;
import com.swp391.techforge.service.warranty.WarrantyTicketService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class WarrantyLookupController {

    private final WarrantyTicketService warrantyTicketService;

    public WarrantyLookupController(WarrantyTicketService warrantyTicketService) {
        this.warrantyTicketService = warrantyTicketService;
    }

    @GetMapping("/warranty-lookup")
    public String lookup(@RequestParam(required = false) String query, Model model) {
        model.addAttribute("query", query);
        if (query != null && !query.isBlank()) {
            List<WarrantyTicket> tickets = warrantyTicketService.lookupForCustomer(query.trim());
            model.addAttribute("tickets", tickets);
            if (tickets.isEmpty()) {
                model.addAttribute("errorMessage", "Không tìm thấy phiếu bảo hành phù hợp.");
            }
        }
        return "customer/warranty-lookup";
    }
}
