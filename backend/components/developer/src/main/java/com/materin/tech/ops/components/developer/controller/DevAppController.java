package com.materin.tech.ops.components.developer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.developer.entity.DevApp;
import com.materin.tech.ops.components.developer.service.DevAppService;
import com.materin.tech.ops.components.developer.vo.DevAppVo;
import jakarta.validation.Valid;
import lombok.Data;
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
 * 开发者应用管理接口（开发者中心）。
 */
@RestController
@RequestMapping("/developer/apps")
@RequiredArgsConstructor
public class DevAppController {

    private final DevAppService devAppService;

    /** 分页查询应用 */
    @GetMapping
    public Result<PageResult<DevAppVo>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(devAppService.page(keyword, status, page, pageSize));
    }

    /** 应用详情 */
    @GetMapping("/{id}")
    public Result<DevAppVo> detail(@PathVariable Long id) {
        return Result.ok(devAppService.detail(id));
    }

    /** 新增应用（自动生成 AppKey/AppSecret） */
    @SaCheckPermission("developer:app:add")
    @PostMapping
    public Result<DevAppVo> create(@Valid @RequestBody DevAppBody body) {
        DevApp app = new DevApp();
        app.setName(body.getName());
        app.setDescription(body.getDescription());
        app.setStatus(body.getStatus());
        return Result.ok(devAppService.create(app));
    }

    /** 编辑应用 */
    @SaCheckPermission("developer:app:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DevAppBody body) {
        DevApp app = new DevApp();
        app.setName(body.getName());
        app.setDescription(body.getDescription());
        app.setStatus(body.getStatus());
        devAppService.update(id, app);
        return Result.ok();
    }

    /** 重置 AppSecret */
    @SaCheckPermission("developer:app:edit")
    @PutMapping("/{id}/secret")
    public Result<DevAppVo> resetSecret(@PathVariable Long id) {
        return Result.ok(devAppService.resetSecret(id));
    }

    /** 删除应用 */
    @SaCheckPermission("developer:app:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        devAppService.delete(id);
        return Result.ok();
    }

    /** 应用请求体 */
    @Data
    public static class DevAppBody {

        private String name;

        private String description;

        private Integer status;
    }
}
