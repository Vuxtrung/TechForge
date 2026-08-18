package com.swp391.techforge.service.buildpc;

import com.swp391.techforge.entity.CompatibilityRule;
import com.swp391.techforge.repository.CompatibilityRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CompatibilityRuleService {

    @Autowired
    private CompatibilityRuleRepository ruleRepository;

    public List<CompatibilityRule> getAllRules() {
        return ruleRepository.findAll();
    }

    public List<CompatibilityRule> getActiveRules() {
        return ruleRepository.findByIsActiveTrue();
    }

    public Optional<CompatibilityRule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    public CompatibilityRule saveRule(CompatibilityRule rule) {
        return ruleRepository.save(rule);
    }

    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    public void toggleRuleStatus(Long id) {
        Optional<CompatibilityRule> optionalRule = ruleRepository.findById(id);
        if (optionalRule.isPresent()) {
            CompatibilityRule rule = optionalRule.get();
            rule.setIsActive(!rule.getIsActive());
            ruleRepository.save(rule);
        }
    }
}
