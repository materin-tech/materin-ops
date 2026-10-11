package com.materin.tech.ops.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置密码请求体。
 */
@Data
public class ResetPasswordBody {

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
