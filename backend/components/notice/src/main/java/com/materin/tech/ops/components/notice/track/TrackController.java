package com.materin.tech.ops.components.notice.track;

import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.api.Result;
import com.mybatisflex.core.paginate.Page;
import com.materin.tech.ops.components.notice.mapper.TrackEventMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.QueryColumn;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 前端埋点：批量上报（匿名可用）+ 统计查询（需登录）。
 */
@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackController {

    private static final QueryColumn C_CREATED_AT = new QueryColumn("created_at");

    private final TrackEventMapper trackEventMapper;

    /** 批量上报（登录与否均允许，便于登录页埋点） */
    @PostMapping("/report")
    public Result<Void> report(@RequestBody List<TrackEvent> events) {
        if (events == null || events.isEmpty()) {
            return Result.ok();
        }
        Long userId = resolveUserIdOrNull();
        LocalDateTime now = LocalDateTime.now();
        for (TrackEvent e : events) {
            e.setId(null);
            e.setUserId(userId);
            e.setCreatedAt(now);
            if (e.getSessionId() == null || e.getSessionId().isBlank()) {
                e.setSessionId("anonymous");
            }
            if (e.getTraceId() != null && e.getTraceId().length() > 64) {
                e.setTraceId(e.getTraceId().substring(0, 64));
            }
            if (e.getProps() != null && e.getProps().length() > 1024) {
                e.setProps(e.getProps().substring(0, 1024));
            }
        }
        trackEventMapper.insertBatch(events);
        return Result.ok();
    }

    /** 分页查询原始事件 */
    @GetMapping("/events")
    public Result<Page<TrackEvent>> events(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize,
                                           @RequestParam(required = false) String event,
                                           @RequestParam(required = false) String sessionId) {
        QueryWrapper qw = QueryWrapper.create();
        if (event != null && !event.isBlank()) {
            qw.and(new QueryColumn("event").eq(event));
        }
        if (sessionId != null && !sessionId.isBlank()) {
            qw.and(new QueryColumn("session_id").eq(sessionId));
        }
        qw.orderBy(C_CREATED_AT.desc());
        return Result.ok(trackEventMapper.paginate(Page.of(page, pageSize), qw));
    }

    /** 页面 PV/UV 聚合（近 N 天） */
    @GetMapping("/page-stats")
    public Result<List<Map<String, Object>>> pageStats(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(trackEventMapper.pageStats(
                LocalDateTime.now().minusDays(days).toString(), 20));
    }

    private Long resolveUserIdOrNull() {
        try {
            String id = StpUtil.getLoginId().toString();
            return id.startsWith("app:") ? null : Long.valueOf(id);
        } catch (Exception e) {
            return null;
        }
    }
}
