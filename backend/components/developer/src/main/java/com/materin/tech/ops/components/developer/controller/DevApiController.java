package com.materin.tech.ops.components.developer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.developer.entity.DevApi;
import com.materin.tech.ops.components.developer.service.DevApiService;
import com.materin.tech.ops.components.developer.vo.DevApiVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放接口管理接口（开发者中心）。接口清单经 knife4j /v3/api-docs 同步初始化。
 */
@RestController
@RequestMapping("/developer/apis")
@RequiredArgsConstructor
public class DevApiController {

    private final DevApiService devApiService;

    /** 分页查询接口清单 */
    @GetMapping
    public Result<PageResult<DevApiVo>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String method,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(devApiService.page(keyword, status, method, page, pageSize));
    }

    /** 从 knife4j 同步接口清单 */
    @SaCheckPermission("developer:api:sync")
    @PostMapping("/sync")
    public Result<DevApiService.SyncResult> sync() {
        return Result.ok(devApiService.sync());
    }

    /** 手工登记接口 */
    @SaCheckPermission("developer:api:edit")
    @PostMapping
    public Result<Void> create(@RequestBody DevApi body) {
        devApiService.create(body);
        return Result.ok();
    }

    /** 编辑接口（名称/分组/开放状态） */
    @SaCheckPermission("developer:api:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DevApi body) {
        devApiService.update(id, body);
        return Result.ok();
    }

    /** 删除接口 */
    @SaCheckPermission("developer:api:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        devApiService.delete(id);
        return Result.ok();
    }
}
