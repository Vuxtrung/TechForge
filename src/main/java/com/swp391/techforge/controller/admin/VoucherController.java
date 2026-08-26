package com.swp391.techforge.controller.admin;

import com.swp391.techforge.entity.Voucher;
import com.swp391.techforge.service.order.VoucherService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    // F_31: View Voucher Ticket List (search/filter/sort/paging trên 1 màn hình)
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) String active,
            @RequestParam(defaultValue = "startDate,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<Voucher> voucherPage = voucherService.search(
                keyword, discountType, active, page, 10, Sort.by(direction, sortParts[0]));

        model.addAttribute("voucherPage", voucherPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("discountType", discountType);
        model.addAttribute("active", active);
        model.addAttribute("sort", sort);
        return "admin/voucher-list";
    }

    // F_30: Update/View Voucher Detail (view + create + update dùng chung 1 form)
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("voucher", new Voucher());
        return "admin/voucher-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Voucher voucher = voucherService.getById(id);
        model.addAttribute("voucher", voucher);
        model.addAttribute("usedCount", voucherService.countUsed(id));
        return "admin/voucher-form";
        
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("voucher") Voucher voucher,
                          BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/voucher-form";
        }
        try {
            voucherService.create(voucher);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/voucher-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Thêm voucher thành công.");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("voucher") Voucher voucher,
                          BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        // Form không tự gửi voucherId về đầy đủ trong mọi trường hợp lỗi;
        // set lại tường minh để voucher-form.html nhận đúng là đang "sửa" (không rơi về form "thêm mới").
        voucher.setVoucherId(id);
        if (result.hasErrors()) {
            model.addAttribute("usedCount", safeCountUsed(id));
            return "admin/voucher-form";
        }
        try {
            voucherService.update(id, voucher);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("usedCount", safeCountUsed(id));
            return "admin/voucher-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật voucher thành công.");
        return "redirect:/admin/vouchers";
    }

    // Tránh văng lỗi ở trang lỗi validate nếu id không còn tồn tại (trường hợp hiếm).
    private long safeCountUsed(Long id) {
        try {
            return voucherService.countUsed(id);
        } catch (IllegalArgumentException ex) {
            return 0L;
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        voucherService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái voucher.");
        return "redirect:/admin/vouchers";
    }
}