package com.materin.tech.ops.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 新增用户请求体。
 */
@Data
public class UserCreateBody {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String nickname;

    private String email;

    private String phone;

    /** 状态：1启用 / 0禁用 */
    private Integer status;

    /** 绑定的角色 ID 集合 */
    private List<Long> roleIds;
}
