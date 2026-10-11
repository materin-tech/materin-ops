package com.materin.tech.ops.components.ops.requirement.dto;

import com.materin.tech.ops.common.api.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 迭代分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = false)
public class IterationPageQuery extends PageParam {

    private Long projectId;

    /** 状态：0未开始 / 1进行中 / 2已结束 */
    private Integer status;
}
