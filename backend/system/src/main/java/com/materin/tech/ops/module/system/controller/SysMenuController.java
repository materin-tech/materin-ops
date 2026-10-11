package com.materin.tech.ops.module.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.module.system.dto.MenuBody;
import com.materin.tech.ops.module.system.service.SysMenuService;
import com.materin.tech.ops.module.system.vo.MenuTreeVo;
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
 * 菜单管理接口。
 */
@RestController
@RequestMapping("/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    /** 全量菜单树（含按钮） */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/tree")
    public Result<List<MenuTreeVo>> tree() {
        return Result.ok(sysMenuService.getMenuTree());
    }

    /** 全量菜单平铺列表 */
    @SaCheckPermission("system:menu:list")
    @GetMapping
    public Result<List<MenuTreeVo>> list() {
        return Result.ok(sysMenuService.listAll());
    }

    /** 新增菜单 */
    @SaCheckPermission("system:menu:add")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody MenuBody body) {
        return Result.ok(sysMenuService.createMenu(body));
    }

    /** 编辑菜单 */
    @SaCheckPermission("system:menu:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MenuBody body) {
        sysMenuService.updateMenu(id, body);
        return Result.ok();
    }

    /** 删除菜单 */
    @SaCheckPermission("system:menu:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.deleteMenu(id);
        return Result.ok();
    }
}
