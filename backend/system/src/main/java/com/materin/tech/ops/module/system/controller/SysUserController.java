package com.materin.tech.ops.module.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.module.system.dto.UserCreateBody;
import com.materin.tech.ops.module.system.dto.UserPageQuery;
import com.materin.tech.ops.module.system.dto.UserUpdateBody;
import com.materin.tech.ops.module.system.dto.ResetPasswordBody;
import com.materin.tech.ops.module.system.service.SysUserService;
import com.materin.tech.ops.module.system.vo.UserVo;
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
 * 用户管理接口。查询仅要求登录，写操作按按钮权限鉴权。
 */
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /** 分页查询用户 */
    @GetMapping
    public Result<PageResult<UserVo>> page(UserPageQuery query) {
        return Result.ok(sysUserService.pageUsers(query));
    }

    /** 新增用户 */
    @SaCheckPermission("system:user:add")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateBody body) {
        sysUserService.createUser(body);
        return Result.ok();
    }

    /** 编辑用户 */
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateBody body) {
        sysUserService.updateUser(id, body);
        return Result.ok();
    }

    /** 重置密码（沿用 system:user:edit 权限标识） */
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordBody body) {
        sysUserService.resetPassword(id, body.getNewPassword());
        return Result.ok();
    }

    /** 启用/禁用用户 */
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        sysUserService.changeStatus(id, status);
        return Result.ok();
    }

    /** 删除用户（逻辑删除） */
    @SaCheckPermission("system:user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.ok();
    }
}
