package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 任务指派请求体。 */
@Data
public class TaskAssignBody {

    @NotNull(message = "被指派人不能为空")
    private Long userId;
}
