package com.materin.tech.ops.common.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知消息模型（跨组件通用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeMessage {

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型：system 系统通知 / announcement 公告 / todo 待办提醒 */
    private String type;

    /** 级别：info / warn / error */
    private String level;

    /** 接收人用户 ID */
    private Long receiverUserId;

    /** 发送者用户 ID（系统发送可为空） */
    private Long senderId;

    /** 点击跳转的前端路由（可空） */
    private String link;
}
