package com.materin.tech.ops.components.ops.requirement.vo;

import lombok.Data;

/** 产品成员视图对象。 */
@Data
public class ProductMemberVo {

    private Long id;

    private Long userId;

    /** 用户昵称（无昵称回显用户名） */
    private String nickname;

    /** 产品角色：owner / qa / dev / member */
    private String role;
}
