package com.swp391.techforge.repository.authentication;

import com.swp391.techforge.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);
}