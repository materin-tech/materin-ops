package com.materin.tech.ops.module.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 编辑用户请求体（不含密码与用户名）。
 */
@Data
public class UserUpdateBody {

    private String nickname;

    private String email;

    private String phone;

    /** 状态：1启用 / 0禁用 */
    private Integer status;

    /** 绑定的角色 ID 集合（全量覆盖重绑） */
    private List<Long> roleIds;
}
