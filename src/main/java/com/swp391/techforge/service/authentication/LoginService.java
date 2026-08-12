package com.swp391.techforge.service.authentication;

import com.swp391.techforge.dto.authentication.LoginRequest;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserStatus;
import com.swp391.techforge.repository.authentication.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email hoặc mật khẩu không chính xác"));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IllegalArgumentException(
                    "Tài khoản của bạn đã bị khóa");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Email hoặc mật khẩu không chính xác");
        }

        return user;
    }
}