package com.swp391.techforge.controller.admin;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserStatus;
import com.swp391.techforge.service.authentication.UserService;
import com.swp391.techforge.util.SortUtil;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
            @RequestParam(defaultValue = "3") int size,
            Model model) {

        if (size < 1) {
            size = 1;
        }

        // Thực hiện tìm kiếm
        Page<User> userPage = userService.search(keyword, roleId, status, page, size,
            SortUtil.parse(sort, "fullName", "asc"));

        // Thêm dữ liệu vào model
        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size);
        model.addAttribute("userStatuses", UserStatus.values());

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
    public String lockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.lockUser(id);
            redirectAttributes.addFlashAttribute("message", "Khóa tài khoản thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/admin/users";
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
        try {
            userService.unlockUser(id);
            redirectAttributes.addFlashAttribute("message", "Mở khóa tài khoản thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/admin/users";
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
}
