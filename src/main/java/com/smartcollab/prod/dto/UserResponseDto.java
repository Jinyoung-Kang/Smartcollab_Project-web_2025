package com.smartcollab.prod.dto;

import com.smartcollab.prod.entity.User;
import lombok.Getter;

@Getter
public class UserResponseDto {
    private final String username;
    private final String name;
    private final String email;

    public UserResponseDto(User user) {
        this.username = user.getUsername();
        this.name = user.getName();
        this.email = user.getEmail();
    }
}
