package com.materin.tech.ops.module.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageParam;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.module.system.dto.RoleCreateBody;
import com.materin.tech.ops.module.system.dto.RoleUpdateBody;
import com.materin.tech.ops.module.system.service.SysRoleService;
import com.materin.tech.ops.module.system.vo.RoleVo;
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
 * 角色管理接口。
 */
@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    /** 分页查询角色（name/code 模糊） */
    @SaCheckPermission("system:role:list")
    @GetMapping
    public Result<PageResult<RoleVo>> page(String name, String code, PageParam param) {
        return Result.ok(sysRoleService.pageRoles(name, code, param));
    }

    /** 全量角色（下拉框用） */
    @GetMapping("/all")
    public Result<List<RoleVo>> listAll() {
        return Result.ok(sysRoleService.listAll());
    }

    /** 新增角色 */
    @SaCheckPermission("system:role:add")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RoleCreateBody body) {
        return Result.ok(sysRoleService.createRole(body));
    }

    /** 编辑角色 */
    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateBody body) {
        sysRoleService.updateRole(id, body);
        return Result.ok();
    }

    /** 分配菜单 */
    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        sysRoleService.assignMenus(id, menuIds);
        return Result.ok();
    }

    /** 删除角色 */
    @SaCheckPermission("system:role:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return Result.ok();
    }
}
