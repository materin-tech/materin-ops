package com.materin.tech.ops.module.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户列表/详情视图（含角色聚合信息）。
 */
@Data
public class UserVo {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    /** 状态：1启用 / 0禁用 */
    private Integer status;

    /** 绑定角色 ID 集合 */
    private List<Long> roleIds;

    /** 绑定角色名称集合 */
    private List<String> roleNames;
}
