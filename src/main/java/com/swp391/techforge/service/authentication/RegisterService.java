package com.swp391.techforge.service.authentication;

import com.swp391.techforge.dto.authentication.RegisterRequest;
import com.swp391.techforge.entity.Role;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserAddress;
import com.swp391.techforge.repository.authentication.RoleRepository;
import com.swp391.techforge.repository.authentication.UserAddressRepository;
import com.swp391.techforge.repository.authentication.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserAddressRepository userAddressRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserAddressRepository userAddressRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userAddressRepository = userAddressRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {

        // 1. Check email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // 2. Check password và confirm password
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu nhập lại không khớp");
        }

        // 3. Lấy role CUSTOMER
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() ->
                        new IllegalStateException("Không tìm thấy role CUSTOMER"));

        // 4. Tạo User
        User user = new User();

        user.setRole(customerRole);
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setPhone(request.getPhone());

        // User.java đã có default
        // ACTIVE và loyaltyPoints = 0

        User savedUser = userRepository.save(user);

        // 5. Tạo địa chỉ
        UserAddress address = new UserAddress();

        address.setUser(savedUser);
        address.setRecipientName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setIsDefault(true);

        userAddressRepository.save(address);
    }
}