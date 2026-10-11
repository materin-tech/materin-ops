package com.materin.tech.ops.components.ops.requirement.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 项目视图对象。 */
@Data
public class ProjectVo {

    private Long id;

    private String name;

    private String code;

    private String description;

    /** 状态：0归档 / 1进行中 / 2已完结 */
    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 关联产品 ID 集合 */
    private List<Long> productIds;

    /** 关联产品名称集合 */
    private List<String> productNames;

}
