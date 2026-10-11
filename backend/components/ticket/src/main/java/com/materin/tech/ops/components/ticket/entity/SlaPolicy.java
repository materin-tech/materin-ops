package com.materin.tech.ops.components.ticket.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * SLA 策略（表 sla_policy）。
 */
@Data
@Table("sla_policy")
public class SlaPolicy {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String name;

    /** 适用优先级：0=P1 ... 3=P4 */
    private Integer priority;

    private Integer firstResponseMinutes;

    private Integer resolveMinutes;

    /** 启用：1是 / 0否 */
    private Integer enabled;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
