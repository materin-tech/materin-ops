package com.materin.tech.ops.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应体（与 vben5 登录响应结构兼容）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVo {

    private String accessToken;

    private String refreshToken;

    private Long userId;

    private String username;

    private String nickname;

    private List<String> roles;
}
