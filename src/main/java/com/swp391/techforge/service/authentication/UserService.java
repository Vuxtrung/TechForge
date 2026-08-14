package com.swp391.techforge.service.authentication;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserStatus;
import com.swp391.techforge.repository.authentication.UserRepository;
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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tìm kiếm người dùng với bộ lọc, tìm kiếm, sắp xếp và phân trang
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

    /**
     * Lấy người dùng theo ID
     */
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }

    /**
     * Lấy người dùng theo email
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Khóa tài khoản người dùng
     */
    @Transactional
    public User lockUser(Long userId) {
        User user = getById(userId);
        user.setStatus(UserStatus.LOCKED);
        return userRepository.save(user);
    }

    /**
     * Mở khóa tài khoản người dùng
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
     * Lưu hoặc cập nhật người dùng
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
