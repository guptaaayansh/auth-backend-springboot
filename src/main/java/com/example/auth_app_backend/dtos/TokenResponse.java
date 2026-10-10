package com.example.auth_app_backend.dtos;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Long expiredIn,
        String tokenType,
        UserDto user
) {

    public static TokenResponse of(String accessToken, String refreshToken, Long expiredIn, UserDto user){
        return new TokenResponse(accessToken, refreshToken, expiredIn, "Bearer", user);
    }

}
