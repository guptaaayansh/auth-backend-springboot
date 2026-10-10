package com.example.auth_app_backend.repositories;

import com.example.auth_app_backend.dtos.UserDto;
import com.example.auth_app_backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

}
