package com.materin.tech.ops.components.ticket.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单操作流水（表 ticket_action）。
 */
@Data
@Table("ticket_action")
public class TicketAction {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long ticketId;

    private Long userId;

    /** 动作：0创建 / 1受理 / 2转派 / 3备注 / 4状态变更 / 5解决 / 6关闭 */
    private Integer action;

    private String content;

    private LocalDateTime createdAt;
}
