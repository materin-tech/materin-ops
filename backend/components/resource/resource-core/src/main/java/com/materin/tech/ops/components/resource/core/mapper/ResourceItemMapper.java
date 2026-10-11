package com.materin.tech.ops.components.resource.core.mapper;

import com.materin.tech.ops.components.resource.core.entity.ResourceItem;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资源元数据 Mapper。
 */
@Mapper
public interface ResourceItemMapper extends BaseMapper<ResourceItem> {
}
