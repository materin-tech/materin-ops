package com.materin.tech.ops.components.ops.requirement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.ops.requirement.dto.ProjectCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.ProjectPageQuery;
import com.materin.tech.ops.components.ops.requirement.dto.ProjectUpdateBody;
import com.materin.tech.ops.components.ops.requirement.service.ProjectService;
import com.materin.tech.ops.components.ops.requirement.vo.ProjectVo;
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

import java.util.List;

/**
 * 项目管理接口。查询仅要求登录，写操作按按钮权限鉴权。
 */
@RestController
@RequestMapping("/requirement/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** 条件分页查询项目 */
    @GetMapping
    public Result<PageResult<ProjectVo>> page(ProjectPageQuery query) {
        return Result.ok(projectService.page(query));
    }

    /** 项目下拉选项（进行中） */
    @GetMapping("/options")
    public Result<List<ProjectVo>> options() {
        return Result.ok(projectService.options());
    }

    /** 项目详情 */
    @GetMapping("/{id}")
    public Result<ProjectVo> detail(@PathVariable Long id) {
        return Result.ok(projectService.detail(id));
    }

    /** 新增项目 */
    @SaCheckPermission("requirement:project:add")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ProjectCreateBody body) {
        projectService.create(body);
        return Result.ok();
    }

    /** 编辑项目 */
    @SaCheckPermission("requirement:project:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateBody body) {
        projectService.update(id, body);
        return Result.ok();
    }

    /** 删除项目 */
    @SaCheckPermission("requirement:project:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }

    /** 项目关联产品 ID 集合 */
    @GetMapping("/{id}/products")
    public Result<List<Long>> productIds(@PathVariable Long id) {
        return Result.ok(projectService.productIds(id));
    }

    /** 全量覆盖项目关联产品 */
    @SaCheckPermission("requirement:project:product")
    @PutMapping("/{id}/products")
    public Result<Void> bindProducts(@PathVariable Long id, @RequestBody List<Long> productIds) {
        projectService.bindProducts(id, productIds);
        return Result.ok();
    }
}
