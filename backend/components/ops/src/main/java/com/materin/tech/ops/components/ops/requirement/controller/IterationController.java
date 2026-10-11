package com.materin.tech.ops.components.ops.requirement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.ops.requirement.dto.IterationCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.IterationPageQuery;
import com.materin.tech.ops.components.ops.requirement.dto.IterationUpdateBody;
import com.materin.tech.ops.components.ops.requirement.service.IterationService;
import com.materin.tech.ops.components.ops.requirement.vo.IterationVo;
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
 * 迭代管理接口。查询仅要求登录，写操作按按钮权限鉴权。
 */
@RestController
@RequestMapping("/requirement/iterations")
@RequiredArgsConstructor
public class IterationController {

    private final IterationService iterationService;

    /** 条件分页查询迭代 */
    @GetMapping
    public Result<PageResult<IterationVo>> page(IterationPageQuery query) {
        return Result.ok(iterationService.page(query));
    }

    /** 迭代详情 */
    @GetMapping("/{id}")
    public Result<IterationVo> detail(@PathVariable Long id) {
        return Result.ok(iterationService.detail(id));
    }

    /** 新增迭代 */
    @SaCheckPermission("requirement:iteration:add")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody IterationCreateBody body) {
        return Result.ok(iterationService.create(body));
    }

    /** 编辑迭代 */
    @SaCheckPermission("requirement:iteration:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody IterationUpdateBody body) {
        iterationService.update(id, body);
        return Result.ok();
    }

    /** 删除迭代 */
    @SaCheckPermission("requirement:iteration:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        iterationService.delete(id);
        return Result.ok();
    }

    /** 迭代状态流转 */
    @SaCheckPermission("requirement:iteration:status")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        iterationService.changeStatus(id, status);
        return Result.ok();
    }
}
