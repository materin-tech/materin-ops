package com.materin.tech.ops.common.api;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用分页请求参数。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PageParam {

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 防御性修正：保证 page/pageSize 在合法范围内 */
    public int safePage() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 500);
    }
}
