package com.swp391.techforge.controller.admin;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserStatus;
import com.swp391.techforge.service.authentication.UserService;
import com.swp391.techforge.util.SortUtil;
import com.swp391.techforge.repository.authentication.RoleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final com.swp391.techforge.service.order.OrderService orderService;

    public UserController(UserService userService, 
                          RoleRepository roleRepository,
                          com.swp391.techforge.service.order.OrderService orderService) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.orderService = orderService;
    }

    /**
     * Hiển thị trang danh sách người dùng dành cho Quản trị viên (Admin).
     * Hỗ trợ tìm kiếm, lọc theo vai trò, lọc theo trạng thái, sắp xếp và phân
     * trang.
     * 
     * @param keyword Từ khóa tìm kiếm (tên, email, số điện thoại)
     * @param roleId  ID của vai trò cần lọc (null = tất cả)
     * @param status  Trạng thái cần lọc (ACTIVE, LOCKED)
     * @param sort    Cú pháp sắp xếp (VD: "fullName,asc")
     * @param page    Số thứ tự trang hiện tại (mặc định 0)
     * @param size    Số lượng bản ghi trên một trang (mặc định 10)
     * @param tab     Tab hiện hành (staff hoặc user)
     * @param model   Đối tượng chứa dữ liệu đẩy ra view
     * @return Tên template HTML hiển thị danh sách người dùng
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "fullName,asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "staff") String tab,
            Model model) {

        if (size < 1) {
            size = 1;
        }

        List<Integer> targetRoleIds;
        if ("user".equals(tab)) {
            // User tab only has CUSTOMER (2)
            targetRoleIds = roleId != null ? List.of(roleId) : List.of(2);
        } else {
            // Staff tab has STAFF_SALES (3), STAFF_WARRANTY (4), ADMIN (5)
            targetRoleIds = roleId != null ? List.of(roleId) : Arrays.asList(3, 4, 5);
        }

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        // Thực hiện tìm kiếm
        Page<User> userPage = userService.searchByRoles(keyword, targetRoleIds, status, page, size,
                Sort.by(direction, sortParts[0]));

        // Thêm dữ liệu vào model
        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size);
        model.addAttribute("activeTab", tab);
        model.addAttribute("userStatuses", UserStatus.values());

        // Fetch roles for the filter dropdown based on current tab
        List<Integer> dropdownRoleIds = "user".equals(tab) ? List.of(2) : Arrays.asList(3, 4, 5);
        model.addAttribute("availableRoles", roleRepository.findAllById(dropdownRoleIds));

        return "admin/user-list";
    }

    /**
     * Khóa tài khoản của một người dùng.
     * Người dùng bị khóa sẽ không thể đăng nhập vào hệ thống.
     * 
     * @param id                 ID của người dùng cần khóa
     * @param redirectAttributes Đối tượng dùng để truyền thông báo (flash message)
     *                           sau khi redirect
     * @return Redirect về trang danh sách người dùng
     */
    @PostMapping("/{id}/lock")
    public String lockUser(@PathVariable Long id, @RequestParam(required = false) String lockReason, RedirectAttributes redirectAttributes) {
        User user = null;
        try {
            user = userService.getById(id);
            userService.lockUser(id, lockReason);
            redirectAttributes.addFlashAttribute("message", "Khóa tài khoản thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/admin/users?tab=" + (user != null && user.getRole() != null && user.getRole().getRoleId() == 2 ? "user" : "staff");
    }

    /**
     * Mở khóa tài khoản của một người dùng đã bị khóa.
     * Phục hồi trạng thái hoạt động bình thường cho tài khoản.
     * 
     * @param id                 ID của người dùng cần mở khóa
     * @param redirectAttributes Đối tượng dùng để truyền thông báo (flash message)
     *                           sau khi redirect
     * @return Redirect về trang danh sách người dùng
     */
    @PostMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = null;
        try {
            user = userService.getById(id);
            userService.unlockUser(id);
            redirectAttributes.addFlashAttribute("message", "Mở khóa tài khoản thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/admin/users?tab=" + (user != null && user.getRole() != null && user.getRole().getRoleId() == 2 ? "user" : "staff");
    }

    /**
     * Xem chi tiết thông tin hồ sơ của một người dùng cụ thể.
     * 
     * @param id    ID của người dùng
     * @param model Đối tượng chứa dữ liệu đẩy ra view
     * @return Tên template HTML hiển thị chi tiết người dùng
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            User user = userService.getById(id);
            model.addAttribute("user", user);
            return "admin/user-detail";
        } catch (Exception e) {
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/{id}/orders-summary")
    @ResponseBody
    public java.util.Map<String, Object> getOrdersSummary(@PathVariable Long id) {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        try {
            User user = userService.getById(id);
            // Lấy danh sách tất cả các đơn hàng của người dùng này
            List<com.swp391.techforge.entity.Order> orders = orderService.getCustomerOrders(user, null, null, null, null);
            
            long pending = orders.stream().filter(o -> o.getStatus() == com.swp391.techforge.entity.OrderStatus.PENDING).count();
            long confirmed = orders.stream().filter(o -> o.getStatus() == com.swp391.techforge.entity.OrderStatus.CONFIRMED).count();
            long shipping = orders.stream().filter(o -> o.getStatus() == com.swp391.techforge.entity.OrderStatus.SHIPPING).count();
            long delivered = orders.stream().filter(o -> 
                o.getStatus() == com.swp391.techforge.entity.OrderStatus.DELIVERED || 
                o.getStatus() == com.swp391.techforge.entity.OrderStatus.COMPLETED
            ).count();
            
            summary.put("success", true);
            summary.put("total", orders.size());
            summary.put("pending", pending);
            summary.put("confirmed", confirmed);
            summary.put("shipping", shipping);
            summary.put("delivered", delivered);
        } catch (Exception e) {
            summary.put("success", false);
            summary.put("message", e.getMessage());
        }
        return summary;
    }
}
