package com.materin.tech.ops.components.ops.requirement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.ops.requirement.dto.StoryCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.StoryPageQuery;
import com.materin.tech.ops.components.ops.requirement.dto.StoryUpdateBody;
import com.materin.tech.ops.components.ops.requirement.service.StoryService;
import com.materin.tech.ops.components.ops.requirement.vo.StoryVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 需求管理接口。查询仅要求登录，写操作按按钮权限鉴权。
 */
@RestController
@RequestMapping("/requirement/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    /** 条件分页查询需求 */
    @GetMapping
    public Result<PageResult<StoryVo>> page(StoryPageQuery query) {
        return Result.ok(storyService.page(query));
    }

    /** 需求详情 */
    @GetMapping("/{id}")
    public Result<StoryVo> detail(@PathVariable Long id) {
        return Result.ok(storyService.detail(id));
    }

    /** 新增需求 */
    @SaCheckPermission("requirement:story:add")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody StoryCreateBody body) {
        return Result.ok(storyService.create(body));
    }

    /** 编辑需求 */
    @SaCheckPermission("requirement:story:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody StoryUpdateBody body) {
        storyService.update(id, body);
        return Result.ok();
    }

    /** 删除需求 */
    @SaCheckPermission("requirement:story:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        storyService.delete(id);
        return Result.ok();
    }

    /** 需求状态流转 */
    @SaCheckPermission("requirement:story:status")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        storyService.changeStatus(id, status);
        return Result.ok();
    }
}
