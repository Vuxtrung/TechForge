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
    public String saveRule(@ModelAttribute CompatibilityRule rule) {
        ruleService.saveRule(rule);
        return "redirect:/admin/compatibility-rules";
    }

    @GetMapping("/edit/{id}")
    public String editRuleForm(@PathVariable Long id, Model model) {
        Optional<CompatibilityRule> rule = ruleService.getRuleById(id);
        if (rule.isPresent()) {
            model.addAttribute("rule", rule.get());
            return "admin/compatibility/form";
        }
        return "redirect:/admin/compatibility-rules";
    }

    @GetMapping("/delete/{id}")
    public String deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return "redirect:/admin/compatibility-rules";
    }

    @GetMapping("/toggle/{id}")
    public String toggleRuleStatus(@PathVariable Long id) {
        ruleService.toggleRuleStatus(id);
        return "redirect:/admin/compatibility-rules";
    }
}
