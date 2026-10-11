package com.materin.tech.ops.components.ops.requirement.dto;

import com.materin.tech.ops.common.api.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 需求分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StoryPageQuery extends PageParam {

    private Long productId;

    private Long moduleId;

    /** 状态：0草稿 / 1评审中 / 2已确认 / 3已关闭 */
    private Integer status;

    /** 优先级：0=P1 ... 3=P4 */
    private Integer priority;

    /** 标题模糊关键字 */
    private String keyword;
}
