package com.example.auth_app_backend.services.Impl;

import com.example.auth_app_backend.dtos.UserDto;
import com.example.auth_app_backend.services.AuthService;
import com.example.auth_app_backend.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto registerUser(UserDto userDto) {

        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

        return userService.createUser(userDto);
    }

}
