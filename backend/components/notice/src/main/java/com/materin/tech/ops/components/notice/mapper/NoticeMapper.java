package com.materin.tech.ops.components.notice.mapper;

import com.materin.tech.ops.components.notice.entity.Notice;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper。
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
