package com.materin.tech.ops.module.system.dto;

import com.materin.tech.ops.common.api.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询参数（username 模糊，status 过滤）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageParam {

    /** 用户名，模糊匹配 */
    private String username;

    /** 状态：1启用 / 0禁用 */
    private Integer status;
}
