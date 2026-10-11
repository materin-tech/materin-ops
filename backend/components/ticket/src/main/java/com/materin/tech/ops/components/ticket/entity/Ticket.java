package com.materin.tech.ops.components.ticket.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单实体（表 ticket）。
 */
@Data
@Table("ticket")
public class Ticket {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String ticketNo;

    private String title;

    private String description;

    /** 类型：0故障 / 1咨询 / 2变更 / 3其他 */
    private Integer type;

    /** 优先级：0=P1 ... 3=P4 */
    private Integer priority;

    /** 状态：0待受理 / 1处理中 / 2待确认 / 3已解决 / 4已关闭 */
    private Integer status;

    private Long reporterId;

    private Long assigneeId;

    private Long slaPolicyId;

    private LocalDateTime responseDeadline;

    private LocalDateTime resolveDeadline;

    private LocalDateTime firstResponseAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
