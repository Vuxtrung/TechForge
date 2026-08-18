package com.swp391.techforge.controller.admin;

import com.swp391.techforge.entity.CompatibilityRule;
import com.swp391.techforge.service.buildpc.CompatibilityRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/compatibility-rules")
public class CompatibilityRuleAdminController {

    @Autowired
    private CompatibilityRuleService ruleService;

    @GetMapping
    public String listRules(Model model) {
        List<CompatibilityRule> rules = ruleService.getAllRules();
        model.addAttribute("rules", rules);
        return "admin/compatibility/list";
    }

    @GetMapping("/create")
    public String createRuleForm(Model model) {
        model.addAttribute("rule", new CompatibilityRule());
        return "admin/compatibility/form";
    }

    @PostMapping("/save")
    public String saveRule(@ModelAttribute CompatibilityRule rule, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            if (rule.getIsActive() == null) rule.setIsActive(true);
            ruleService.saveRule(rule);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu quy tắc thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu quy tắc: " + e.getMessage());
        }
        return "redirect:/admin/compatibility-rules";
    }

    @GetMapping("/edit/{id}")
    public String editRuleForm(@PathVariable Long id, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Optional<CompatibilityRule> rule = ruleService.getRuleById(id);
        if (rule.isPresent()) {
            model.addAttribute("rule", rule.get());
            return "admin/compatibility/form";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy quy tắc này!");
        return "redirect:/admin/compatibility-rules";
    }

    @GetMapping("/delete/{id}")
    public String deleteRule(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            ruleService.deleteRule(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa quy tắc!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa quy tắc!");
        }
        return "redirect:/admin/compatibility-rules";
    }

    @GetMapping("/toggle/{id}")
    public String toggleRuleStatus(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            ruleService.toggleRuleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái quy tắc!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái!");
        }
        return "redirect:/admin/compatibility-rules";
    }
}
