package com.materin.tech.ops.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户信息响应体（与 vben5 /user/info 响应结构兼容）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVo {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private List<String> roles;

    private List<String> permissions;
}
