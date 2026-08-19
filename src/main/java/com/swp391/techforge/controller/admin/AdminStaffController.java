package com.swp391.techforge.controller.admin;

import com.swp391.techforge.dto.admin.StaffCreateRequest;
import com.swp391.techforge.entity.Role;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserStatus;
import com.swp391.techforge.repository.authentication.RoleRepository;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.authentication.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Default staff role IDs
    private final List<Integer> STAFF_ROLE_IDS = Arrays.asList(3, 4);

    public AdminStaffController(UserService userService, RoleRepository roleRepository,
                                UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hiển thị trang danh sách nhân viên (Staff) dành cho Quản trị viên.
     * Lọc riêng các tài khoản có role là STAFF_SALES (3) hoặc STAFF_WARRANTY (4).
     * Có hỗ trợ tìm kiếm, sắp xếp và phân trang tương tự trang người dùng.
     * 
     * @param keyword
     * @param roleId
     * @param status
     * @param sort
     * @param page
     * @param size 
     * @param model
     * @return
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "fullName,asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        if (size < 1) {
            size = 1;
        }

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        List<Integer> targetRoleIds = roleId != null ? List.of(roleId) : STAFF_ROLE_IDS;

        Page<User> staffPage = userService.searchByRoles(keyword, targetRoleIds, status, page, size,
                Sort.by(direction, sortParts[0]));

        model.addAttribute("staffPage", staffPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size);
        model.addAttribute("userStatuses", UserStatus.values());
        model.addAttribute("staffRoles", roleRepository.findAllById(STAFF_ROLE_IDS));

        return "admin/staff-list";
    }

    /**
     * Hiển thị giao diện Form thêm nhân viên mới.
     * Load sẵn danh sách các Role hợp lệ để chọn (chỉ bao gồm các role Staff).
     * 
     * @param model Đối tượng đẩy dữ liệu ra view
     * @return Template form thêm nhân viên
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("staffCreateRequest", new StaffCreateRequest());
        model.addAttribute("staffRoles", roleRepository.findAllById(STAFF_ROLE_IDS));
        return "admin/staff-form";
    }

    /**
     * Xử lý dữ liệu khi Admin submit form thêm nhân viên mới.
     * Kiểm tra tính hợp lệ của dữ liệu (email trùng, role không đúng...).
     * Nếu hợp lệ thì tiến hành mã hóa mật khẩu và tạo tài khoản nhân viên mới vào DB.
     * 
     * @param request
     * @param bindingResult 
     * @param model 
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/add")
    public String processAddStaff(
            @Valid @ModelAttribute("staffCreateRequest") StaffCreateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userRepository.existsByEmail(request.getEmail())) {
            bindingResult.rejectValue("email", "error.email", "Email này đã được sử dụng");
        }

        if (!STAFF_ROLE_IDS.contains(request.getRoleId())) {
            bindingResult.rejectValue("roleId", "error.roleId", "Vai trò không hợp lệ");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("staffRoles", roleRepository.findAllById(STAFF_ROLE_IDS));
            return "admin/staff-form";
        }

        Role staffRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò"));

        User staff = new User();
        staff.setFullName(request.getFullName());
        staff.setEmail(request.getEmail());
        staff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        staff.setPhone(request.getPhone());
        staff.setRole(staffRole);
        staff.setStatus(UserStatus.ACTIVE);

        userService.save(staff);

        redirectAttributes.addFlashAttribute("message", "Thêm nhân viên thành công!");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/admin/staff";
    }
}
