package com.materin.tech.ops.module.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.module.system.dto.LoginBody;
import com.materin.tech.ops.module.system.dto.LoginVo;
import com.materin.tech.ops.module.system.dto.UserInfoVo;
import com.materin.tech.ops.module.system.service.AuthService;
import com.materin.tech.ops.module.system.service.SysMenuService;
import com.materin.tech.ops.module.system.vo.RouteVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口：登录 / 登出 / 用户信息 / 动态路由。
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysMenuService sysMenuService;

    /** 登录（匿名访问） */
    @PostMapping("/auth/login")
    public Result<LoginVo> login(@Valid @RequestBody LoginBody body) {
        return Result.ok(authService.login(body));
    }

    /** 登出 */
    @SaCheckLogin
    @PostMapping("/auth/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /** 当前登录用户信息（vben5 约定路径 /user/info） */
    @SaCheckLogin
    @GetMapping("/user/info")
    public Result<UserInfoVo> getUserInfo() {
        return Result.ok(authService.getUserInfo(StpUtil.getLoginIdAsLong()));
    }

    /** 当前用户动态路由 */
    @SaCheckLogin
    @GetMapping("/auth/routes")
    public Result<List<RouteVo>> getRoutes() {
        return Result.ok(sysMenuService.getRoutes(StpUtil.getLoginIdAsLong()));
    }

    /** 刷新 token（Sa-Token 单 token 模式：续期并原样返回当前 token） */
    @SaCheckLogin
    @PostMapping("/auth/refresh")
    public Result<LoginVo> refresh() {
        long userId = StpUtil.getLoginIdAsLong();
        StpUtil.renewTimeout(StpUtil.getTokenTimeout());
        return Result.ok(authService.buildLoginVo(userId));
    }

    /** 当前用户权限标识集合（vben5 前端按钮级权限 codes） */
    @SaCheckLogin
    @GetMapping("/auth/codes")
    public Result<List<String>> getCodes() {
        return Result.ok(authService.getPermissionCodes(StpUtil.getLoginIdAsLong()));
    }
}
