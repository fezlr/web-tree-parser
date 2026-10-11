package com.fezlr.web_tree_parser.admin.mapper;

import com.fezlr.web_tree_parser.admin.dto.response.UserInfoResponse;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class AdminUserMapper {
    public UserInfoResponse toResponse(UserEntity user, ZoneId zone) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(LocalDateTime.ofInstant(user.getCreatedAt(), zone))
                .build();
    }
}
