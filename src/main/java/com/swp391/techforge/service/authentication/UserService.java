package com.swp391.techforge.service.authentication;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserStatus;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.order.OrderService;
import com.swp391.techforge.service.warranty.WarrantyTicketService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderService orderService;
    private final WarrantyTicketService warrantyTicketService;

    public UserService(UserRepository userRepository, 
                       @Lazy OrderService orderService, 
                       @Lazy WarrantyTicketService warrantyTicketService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.warrantyTicketService = warrantyTicketService;
    }

    /**
     * Tìm kiếm người dùng với bộ lọc, tìm kiếm, sắp xếp và phân trang.
     * Sử dụng cho việc lấy danh sách người dùng hiển thị trên trang Quản trị.
     * 
     * @param keyword Từ khóa tìm kiếm theo tên, email hoặc SĐT
     * @param roleId Lọc theo nhóm quyền (nếu null lấy tất cả)
     * @param status Trạng thái tài khoản (ACTIVE, LOCKED)
     * @param page Trang số mấy
     * @param size Số bản ghi mỗi trang
     * @param sort Đối tượng Sort chỉ định chiều và trường cần sắp xếp
     * @return Trang kết quả chứa danh sách người dùng
     */
    @Transactional(readOnly = true)
    public Page<User> search(String keyword, Integer roleId, String status,
                             int page, int size, Sort sort) {
        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                userStatus = UserStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                userStatus = null;
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.search(keyword, roleId, userStatus, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchByRoles(String keyword, java.util.List<Integer> roleIds, String status,
                                    int page, int size, Sort sort) {
        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                userStatus = UserStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                userStatus = null;
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.searchByRoles(keyword, roleIds, userStatus, pageable);
    }

    /**
     * Lấy người dùng theo ID
     */
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }

    /**
     * Lấy người dùng theo email. Thường dùng trong chức năng Đăng nhập.
     * 
     * @param email Địa chỉ email của người dùng
     * @return Đối tượng Optional bọc User
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Khóa tài khoản người dùng, chuyển trạng thái sang LOCKED.
     * Người dùng này sẽ không thể đăng nhập. Ngăn chặn việc khóa Admin.
     * Tự động hủy đơn hàng PENDING đối với Customer và gỡ ticket đối với Staff.
     * 
     * @param userId ID người dùng
     * @return User sau khi khóa
     */
    @Transactional
    public User lockUser(Long userId) {
        User user = getById(userId);
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new IllegalStateException("Không thể khóa tài khoản quản trị viên (ADMIN)!");
        }
        user.setStatus(UserStatus.LOCKED);
        User savedUser = userRepository.save(user);

        // Xử lý logic nghiệp vụ khi khóa
        if (user.getRole() != null) {
            String roleName = user.getRole().getRoleName();
            if ("CUSTOMER".equalsIgnoreCase(roleName)) {
                orderService.cancelPendingOrdersForUser(savedUser);
            } else if ("STAFF_WARRANTY".equalsIgnoreCase(roleName)) {
                warrantyTicketService.unassignTicketsFromStaff(savedUser);
            }
        }

        return savedUser;
    }

    /**
     * Mở khóa tài khoản người dùng, chuyển trạng thái lại thành ACTIVE.
     * 
     * @param userId ID người dùng
     * @return User sau khi mở khóa
     */
    @Transactional
    public User unlockUser(Long userId) {
        User user = getById(userId);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    /**
     * Cập nhật trạng thái người dùng
     */
    @Transactional
    public User updateStatus(Long userId, UserStatus status) {
        User user = getById(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    /**
     * Lưu hoặc cập nhật người dùng vào CSDL.
     * 
     * @param user Đối tượng người dùng
     * @return User đã lưu
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Xóa người dùng
     */
    @Transactional
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }
}
