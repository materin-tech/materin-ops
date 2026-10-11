package com.materin.tech.ops.components.notice.mapper;

import com.materin.tech.ops.components.notice.entity.NotifyChannel;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知渠道 Mapper。
 */
@Mapper
public interface NotifyChannelMapper extends BaseMapper<NotifyChannel> {
}
