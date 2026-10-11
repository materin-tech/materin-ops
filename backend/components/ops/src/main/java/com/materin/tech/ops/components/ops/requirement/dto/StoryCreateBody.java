package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增需求请求体（初始状态为草稿，状态流转走独立接口）。 */
@Data
public class StoryCreateBody {

    @NotNull(message = "所属产品不能为空")
    private Long productId;

    /** 所属产品模块 ID，可空 */
    private Long moduleId;

    @NotBlank(message = "需求标题不能为空")
    @Size(max = 256, message = "需求标题过长")
    private String title;

    private String description;

    /** 优先级：0=P1 ... 3=P4 */
    private Integer priority;

    /** 需求负责人用户 ID */
    private Long ownerId;

    private Integer sort;
}
