package com.fezlr.web_tree_parser.admin.dto.response;

import com.fezlr.web_tree_parser.user.entity.Role;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserInfoResponse(

        Long id,

        String username,

        Role role,

        boolean enabled,

        LocalDateTime createdAt

) {
}
