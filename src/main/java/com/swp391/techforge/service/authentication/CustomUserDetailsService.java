package com.swp391.techforge.service.authentication;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserStatus;
import com.swp391.techforge.repository.authentication.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        public CustomUserDetailsService(UserRepository userRepository) {
                this.userRepository = userRepository;
        }

        @Override
        public UserDetails loadUserByUsername(String email)
                        throws UsernameNotFoundException {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = 
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()));

                return new CustomUserDetails(
                        user.getEmail(),
                        user.getPasswordHash(),
                        user.getStatus() == UserStatus.ACTIVE,
                        true, true, true,
                        authorities,
                        user.getFullName()
                );
        }
}