package com.swp391.techforge.service.authentication;

import com.swp391.techforge.dto.authentication.RegisterRequest;
import com.swp391.techforge.entity.AddressType;
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
    private final UserAddressRepository userAddressRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(
            UserRepository userRepository,
            UserAddressRepository userAddressRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void validate(RegisterRequest request) {
        // 1. Check email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // 2. Check password và confirm password
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu nhập lại không khớp");
        }
    }

    @Transactional
    public void register(RegisterRequest request) {

        // Kiểm tra lại lần nữa phòng trường hợp email đã bị đăng ký trong lúc chờ OTP
        validate(request);

        // 3. Lấy role CUSTOMER
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role CUSTOMER"));

        // 4. Tạo User
        User user = new User();
        user.setRole(customerRole);
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        // default status = ACTIVE
        // default loyaltyPoints = 0
        userRepository.save(user);

        // 5. Tạo địa chỉ đầu tiên cho User
        UserAddress address = new UserAddress();
        address.setUser(user);
        address.setRecipientName(user.getFullName());
        address.setPhone(user.getPhone());
        address.setProvince(request.getProvince());
        address.setWard(request.getWard());
        address.setAddressLine(request.getAddressLine());
        address.setAddressType(AddressType.HOME);
        address.setIsDefault(true);
        userAddressRepository.save(address);
    }
}