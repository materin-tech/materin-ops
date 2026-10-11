package com.materin.tech.ops.components.developer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.developer.service.DevAuthService;
import com.materin.tech.ops.components.developer.vo.DevAuthVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 应用-接口授权管理接口（开发者中心）。
 */
@RestController
@RequestMapping("/developer/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevAuthService devAuthService;

    /** 应用授权详情（全部接口 + 已授权集合） */
    @GetMapping("/apps/{appId}/apis")
    public Result<DevAuthVo> detail(@PathVariable Long appId) {
        return Result.ok(devAuthService.detail(appId));
    }

    /** 全量覆盖应用接口授权 */
    @SaCheckPermission("developer:auth:grant")
    @PutMapping("/apps/{appId}/apis")
    public Result<Void> saveGrant(@PathVariable Long appId, @RequestBody List<Long> apiIds) {
        devAuthService.saveGrant(appId, apiIds);
        return Result.ok();
    }
}
