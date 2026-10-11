package com.materin.tech.ops.components.ticket.mapper;

import com.materin.tech.ops.components.ticket.entity.SlaPolicy;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * SlaPolicy Mapper。
 */
@Mapper
public interface SlaPolicyMapper extends BaseMapper<SlaPolicy> {
}
