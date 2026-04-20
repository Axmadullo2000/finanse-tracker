package com.fintrack.mapper;

import com.fintrack.dto.UserResponse;
import com.fintrack.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
