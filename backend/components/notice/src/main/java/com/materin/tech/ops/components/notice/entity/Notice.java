package com.materin.tech.ops.components.notice.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知。
 */
@Data
@Table("notice")
public class Notice {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String title;

    private String content;

    /** 类型：system / announcement / todo */
    private String type;

    /** 级别：info / warn / error */
    private String level;

    private Long receiverId;

    private Long senderId;

    /** 点击跳转的前端路由 */
    private String link;

    /** 已读：0未读 / 1已读 */
    private Integer readFlag;

    private LocalDateTime readAt;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
