package com.materin.tech.ops.components.ops.requirement.vo;

import lombok.Data;


/** 需求视图对象。 */
@Data
public class StoryVo {

    private Long id;

    private Long productId;

    private String productName;

    private Long moduleId;

    private String moduleTitle;

    private String title;

    private String description;

    /** 状态：0草稿 / 1评审中 / 2已确认 / 3已关闭 */
    private Integer status;

    /** 优先级：0=P1 ... 3=P4 */
    private Integer priority;

    private Long ownerId;

    /** 负责人昵称 */
    private String ownerName;

    private Integer sort;

}
