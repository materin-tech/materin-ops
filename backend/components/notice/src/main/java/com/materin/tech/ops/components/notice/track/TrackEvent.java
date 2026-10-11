package com.materin.tech.ops.components.notice.track;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 前端埋点事件。
 */
@Data
@Table("track_event")
public class TrackEvent {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String traceId;

    private String sessionId;

    private Long userId;

    /** 端：web / h5 */
    private String client;

    /** 事件类型：pv / click / custom */
    private String event;

    private String page;

    private String target;

    /** 扩展属性 JSON */
    private String props;

    private String ua;

    private LocalDateTime createdAt;
}
