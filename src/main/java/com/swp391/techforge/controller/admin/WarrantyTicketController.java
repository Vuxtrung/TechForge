package com.swp391.techforge.controller.admin;

import com.swp391.techforge.entity.WarrantyTicket;
import com.swp391.techforge.entity.WarrantyTicketStatus;
import com.swp391.techforge.service.warranty.WarrantyTicketService;
import com.swp391.techforge.util.ExcelExportUtil;
import com.swp391.techforge.util.SortUtil;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Controller
@RequestMapping("/admin/warranty")
public class WarrantyTicketController {

    private final WarrantyTicketService warrantyTicketService;

    public WarrantyTicketController(WarrantyTicketService warrantyTicketService) {
        this.warrantyTicketService = warrantyTicketService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<WarrantyTicket> ticketPage = warrantyTicketService.search(
                keyword, status, page, size, SortUtil.parse(sort, "createdAt", "desc"));
        model.addAttribute("ticketPage", ticketPage);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("statuses", WarrantyTicketStatus.values());
        return "admin/warranty-list";
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            ByteArrayOutputStream outputStream = ExcelExportUtil.exportWarrantyTicketsToExcel(
                    warrantyTicketService.getAllForExport(keyword, status));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "warranty-tickets.xlsx");
            return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Export failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        WarrantyTicket ticket = warrantyTicketService.getById(id);
        model.addAttribute("ticket", ticket);
        model.addAttribute("statuses", WarrantyTicketStatus.values());
        return "admin/warranty-detail";
    }

    @PostMapping("/receive")
    public String receive(@RequestParam Long userId,
            @RequestParam String imeiSerial,
            @RequestParam(required = false) String phoneLookup,
            @RequestParam(required = false) String issueDesc,
            @RequestParam(required = false) Long orderItemId,
            RedirectAttributes redirectAttributes) {
        try {
            warrantyTicketService.receiveProduct(userId, imeiSerial, phoneLookup, issueDesc, orderItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Tiếp nhận sản phẩm bảo hành thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/warranty";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedStaffId,
            RedirectAttributes redirectAttributes) {
        try {
            warrantyTicketService.updateProgress(id, status, assignedStaffId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tiến độ kỹ thuật thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/warranty/" + id;
    }

    @PostMapping("/{id}/replace")
    public String replace(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            warrantyTicketService.markReplaced1For1(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xử lý 1 đổi 1 thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/warranty/" + id;
    }

    @PostMapping("/{id}/repair")
    public String repair(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            warrantyTicketService.markRepaired(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái sửa chữa thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/warranty/" + id;
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            warrantyTicketService.closeTicket(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đóng phiếu bảo hành.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/warranty/" + id;
    }
}
