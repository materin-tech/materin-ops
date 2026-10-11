package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 编辑需求请求体（不含产品与状态，状态流转走独立接口）。 */
@Data
public class StoryUpdateBody {

    private Long moduleId;

    @Size(max = 256, message = "需求标题过长")
    private String title;

    private String description;

    private Integer priority;

    private Long ownerId;

    private Integer sort;
}
