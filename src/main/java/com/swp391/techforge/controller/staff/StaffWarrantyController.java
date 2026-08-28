package com.swp391.techforge.controller.staff;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.WarrantyTicket;
import com.swp391.techforge.entity.WarrantyTicketStatus;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.warranty.WarrantyTicketService;
import com.swp391.techforge.util.SortUtil;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/staff/warranty")
public class StaffWarrantyController {
    private final WarrantyTicketService warrantyTicketService;
    private final UserRepository userRepository;

    public StaffWarrantyController(WarrantyTicketService warrantyTicketService, UserRepository userRepository) {
        this.warrantyTicketService = warrantyTicketService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Model model) {
        Page<WarrantyTicket> ticketPage = warrantyTicketService.search(keyword, status, page, size,
                SortUtil.parse(sort, "createdAt", "desc"));
        model.addAttribute("ticketPage", ticketPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size);
        model.addAttribute("statuses", WarrantyTicketStatus.values());
        return "staff/warranty-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        List<User> customers = userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().getRoleId() == 2)
                .toList();
        model.addAttribute("customers", customers);
        return "staff/warranty-create";
    }

    @PostMapping("/receive")
    public String receive(@RequestParam Long userId, @RequestParam String imeiSerial,
            @RequestParam(required = false) String phoneLookup, @RequestParam(required = false) String issueDesc,
            @RequestParam(required = false) Long orderItemId, RedirectAttributes redirectAttributes) {
        try {
            warrantyTicketService.receiveProduct(userId, imeiSerial, phoneLookup, issueDesc, orderItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tiếp nhận sản phẩm và tạo phiếu bảo hành.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/warranty";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("ticket", warrantyTicketService.getById(id));
        model.addAttribute("statuses", WarrantyTicketStatus.values());
        List<User> staffMembers = userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && (user.getRole().getRoleId() == 3 || user.getRole().getRoleId() == 4))
                .toList();
        model.addAttribute("staffMembers", staffMembers);
        return "staff/warranty-detail";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedStaffId, RedirectAttributes redirectAttributes) {
        try {
            warrantyTicketService.updateProgress(id, status, assignedStaffId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phiếu bảo hành thành công.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/warranty/" + id;
    }
}
