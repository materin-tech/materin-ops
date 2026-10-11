package com.materin.tech.ops.components.ops.requirement.dto;

import com.materin.tech.ops.common.api.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 任务分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TaskPageQuery extends PageParam {

    private Long projectId;

    private Long iterationId;

    /** 状态：0待处理 / 1进行中 / 2已完成 / 3已关闭 */
    private Integer status;

    /** 类型：0开发 / 1测试 / 2设计 / 3其他 */
    private Integer type;

    /** 指派给用户 ID */
    private Long assigneeId;

    /** 标题模糊关键字 */
    private String keyword;
}
