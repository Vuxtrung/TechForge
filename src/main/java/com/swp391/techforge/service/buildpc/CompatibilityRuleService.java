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

    /**
     * Lấy danh sách toàn bộ các quy tắc tương thích (kể cả đã tắt hay đang bật).
     * Phục vụ cho trang quản trị hệ thống (Admin).
     * 
     * @return Danh sách tất cả quy tắc
     */
    public List<CompatibilityRule> getAllRules() {
        return ruleRepository.findAll();
    }

    /**
     * Lấy danh sách các quy tắc ĐANG HOẠT ĐỘNG (isActive = true).
     * Được sử dụng bởi thuật toán kiểm tra cấu hình máy tính của khách hàng.
     * 
     * @return Danh sách các quy tắc đang hoạt động
     */
    public List<CompatibilityRule> getActiveRules() {
        return ruleRepository.findByIsActiveTrue();
    }

    public Optional<CompatibilityRule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    /**
     * Lưu một quy tắc mới hoặc cập nhật một quy tắc đã có vào Cơ sở dữ liệu.
     * 
     * @param rule Đối tượng quy tắc cần lưu
     * @return Đối tượng quy tắc sau khi đã lưu thành công (có ID)
     */
    public CompatibilityRule saveRule(CompatibilityRule rule) {
        return ruleRepository.save(rule);
    }

    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    /**
     * Thay đổi trạng thái Hoạt động/Vô hiệu hóa của một quy tắc.
     * 
     * @param id ID của quy tắc cần thay đổi trạng thái
     */
    public void toggleRuleStatus(Long id) {
        Optional<CompatibilityRule> optionalRule = ruleRepository.findById(id);
        if (optionalRule.isPresent()) {
            CompatibilityRule rule = optionalRule.get();
            if (rule.getIsActive() == null) {
                rule.setIsActive(false); // default to false if it was null when toggling
            } else {
                rule.setIsActive(!rule.getIsActive());
            }
            ruleRepository.save(rule);
        }
    }
}
