package com.swp391.techforge.repository.authentication;

import com.swp391.techforge.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
}