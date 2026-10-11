package com.materin.tech.ops.components.developer.vo;

import lombok.Data;

import java.util.List;

/** 应用授权视图：全部接口清单 + 已授权接口 ID 集合。 */
@Data
public class DevAuthVo {

    private Long appId;

    private List<Long> authorizedApiIds;

    private List<DevApiVo> apis;
}
