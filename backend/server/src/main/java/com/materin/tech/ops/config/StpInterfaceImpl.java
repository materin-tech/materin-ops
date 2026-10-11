package com.materin.tech.ops.config;

import cn.dev33.satoken.stp.StpInterface;
import com.materin.tech.ops.module.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限数据源：为注解鉴权提供当前用户的权限与角色集合。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final AuthService authService;

    /** 返回当前用户权限标识集合（super_admin / APP 虚拟身份返回 *:*:* 全量标记） */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String id = loginId.toString();
        // 开放 API 的 APP 虚拟身份：接口级白名单由 OpenApiAuthInterceptor 控制，这里全放行
        if (id.startsWith("app:")) {
            return List.of("*:*:*");
        }
        Long userId = Long.valueOf(id);
        return authService.getPermissionCodes(userId);
    }

    /** 返回当前用户角色编码集合 */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String id = loginId.toString();
        if (id.startsWith("app:")) {
            return List.of();
        }
        Long userId = Long.valueOf(id);
        return authService.getRoleCodes(userId);
    }
}
