package com.materin.tech.ops.components.ops.requirement.dto;

import com.materin.tech.ops.common.api.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 项目分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProjectPageQuery extends PageParam {

    /** 状态：0归档 / 1进行中 / 2已完结 */
    private Integer status;

    /** 名称/编码模糊关键字 */
    private String keyword;
}
