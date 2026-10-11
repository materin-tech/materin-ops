package com.materin.tech.ops.components.ops.requirement.mapper;

import com.materin.tech.ops.components.ops.requirement.entity.Story;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Story Mapper。
 */
@Mapper
public interface StoryMapper extends BaseMapper<Story> {
}
