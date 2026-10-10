package com.example.auth_app_backend.services;

import com.example.auth_app_backend.dtos.UserDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserService {

    //create user
    UserDto createUser(UserDto userDto);

    //get user by email
    UserDto getUserByEmail(String email);

    //update user
    UserDto updateUser(UserDto userDto, String userId);

    //delete user
    void deleteUser(String userId);

    //get user by id
    UserDto getUserById(String userId);

    //get all users
    Iterable<UserDto> getAllUsers();

}
