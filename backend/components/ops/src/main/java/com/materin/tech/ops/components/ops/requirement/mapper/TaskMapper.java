package com.materin.tech.ops.components.ops.requirement.mapper;

import com.materin.tech.ops.components.ops.requirement.entity.Task;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Task Mapper。
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
