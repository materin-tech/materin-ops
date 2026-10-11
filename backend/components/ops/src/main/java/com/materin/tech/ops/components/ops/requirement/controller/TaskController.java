package com.materin.tech.ops.components.ops.requirement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.ops.requirement.dto.TaskAssignBody;
import com.materin.tech.ops.components.ops.requirement.dto.TaskCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.TaskPageQuery;
import com.materin.tech.ops.components.ops.requirement.dto.TaskUpdateBody;
import com.materin.tech.ops.components.ops.requirement.service.TaskService;
import com.materin.tech.ops.components.ops.requirement.vo.TaskVo;
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
 * 任务管理接口。查询仅要求登录，写操作按按钮权限鉴权。
 */
@RestController
@RequestMapping("/requirement/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** 条件分页查询任务 */
    @GetMapping
    public Result<PageResult<TaskVo>> page(TaskPageQuery query) {
        return Result.ok(taskService.page(query));
    }

    /** 按需求查询任务列表 */
    @GetMapping("/by-story/{storyId}")
    public Result<List<TaskVo>> listByStory(@PathVariable Long storyId) {
        return Result.ok(taskService.listByStory(storyId));
    }

    /** 任务详情 */
    @GetMapping("/{id}")
    public Result<TaskVo> detail(@PathVariable Long id) {
        return Result.ok(taskService.detail(id));
    }

    /** 新增任务 */
    @SaCheckPermission("requirement:task:add")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TaskCreateBody body) {
        return Result.ok(taskService.create(body));
    }

    /** 编辑任务 */
    @SaCheckPermission("requirement:task:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateBody body) {
        taskService.update(id, body);
        return Result.ok();
    }

    /** 删除任务 */
    @SaCheckPermission("requirement:task:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.ok();
    }

    /** 任务状态流转 */
    @SaCheckPermission("requirement:task:status")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        taskService.changeStatus(id, status);
        return Result.ok();
    }

    /** 任务指派 */
    @SaCheckPermission("requirement:task:assign")
    @PutMapping("/{id}/assignee")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody TaskAssignBody body) {
        taskService.assign(id, body.getUserId());
        return Result.ok();
    }
}
