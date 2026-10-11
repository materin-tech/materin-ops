package com.materin.tech.ops.components.notice.mapper;

import com.materin.tech.ops.components.notice.track.TrackEvent;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 埋点事件 Mapper。
 */
@Mapper
public interface TrackEventMapper extends BaseMapper<TrackEvent> {

    /** 按页面聚合 PV */
    @Select("SELECT page AS page, COUNT(*) AS pv, COUNT(DISTINCT session_id) AS uv "
            + "FROM track_event WHERE event = 'pv' AND created_at >= #{since} "
            + "GROUP BY page ORDER BY pv DESC LIMIT #{limit}")
    List<Map<String, Object>> pageStats(String since, int limit);
}
