package com.materin.tech.ops.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 新增角色请求体。
 */
@Data
public class RoleCreateBody {

    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    private Integer sort;

    /** 状态：1启用 / 0禁用 */
    private Integer status;

    private String remark;

    /** 绑定的菜单 ID 集合（全量覆盖） */
    private List<Long> menuIds;
}
