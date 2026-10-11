package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 产品成员全量覆盖保存请求体。 */
@Data
public class ProductMemberBody {

    @NotEmpty(message = "成员列表不能为空")
    private List<MemberItem> members;

    @Data
    public static class MemberItem {

        private Long userId;

        /** 产品角色：owner / qa / dev / member */
        @Size(max = 32, message = "产品角色过长")
        private String role;
    }
}
