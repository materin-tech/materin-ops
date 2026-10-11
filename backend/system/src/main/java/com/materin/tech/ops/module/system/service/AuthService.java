package com.materin.tech.ops.module.system.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.module.system.dto.LoginBody;
import com.materin.tech.ops.module.system.dto.LoginVo;
import com.materin.tech.ops.module.system.dto.UserInfoVo;
import com.materin.tech.ops.module.system.entity.SysMenu;
import com.materin.tech.ops.module.system.mapper.SysMenuMapper;
import com.materin.tech.ops.module.system.mapper.SysRoleMapper;
import com.materin.tech.ops.module.system.mapper.SysRoleMenuMapper;
import com.materin.tech.ops.module.system.mapper.SysUserMapper;
import com.materin.tech.ops.module.system.mapper.SysUserRoleMapper;
import com.materin.tech.ops.module.system.entity.SysRole;
import com.materin.tech.ops.module.system.entity.SysUser;
import com.materin.tech.ops.module.system.entity.SysRoleMenu;
import com.materin.tech.ops.module.system.entity.SysUserRole;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务：登录 / 登出 / 用户信息 / 权限与角色聚合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 超级管理员角色编码：拥有全量权限 */
    public static final String SUPER_ADMIN = "super_admin";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    // ---------- 表列常量（不依赖 APT 生成的 TableDef） ----------
    private static final QueryColumn USERNAME = new QueryColumn("username");
    private static final QueryColumn USER_ID = new QueryColumn("user_id");
    private static final QueryColumn ROLE_ID = new QueryColumn("role_id");
    private static final QueryColumn MENU_ID = new QueryColumn("menu_id");
    private static final QueryColumn STATUS = new QueryColumn("status");

    /**
     * 登录：校验账号状态与密码，Sa-Token 登录后返回 token 与角色信息。
     */
    public LoginVo login(LoginBody body) {
        // username 在库中唯一（含已删除防重），直接等值查询即可
        SysUser user = sysUserMapper.selectOneByQuery(
                QueryWrapper.create().where(USERNAME.eq(body.getUsername())));
        // 不区分"用户不存在"与"密码错误"，避免账号枚举
        if (user == null || !BCrypt.checkpw(body.getPassword(), user.getPassword())
                || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或密码错误");
        }
        StpUtil.login(user.getId());
        return buildLoginVo(user.getId());
    }

    /**
     * 构建登录响应（token、账号、角色集合）。
     */
    public LoginVo buildLoginVo(Long userId) {
        SysUser user = sysUserMapper.selectOneById(userId);
        return LoginVo.builder()
                .accessToken(StpUtil.getTokenValue())
                .refreshToken(StpUtil.getTokenValue())
                .userId(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(getRoleCodes(userId))
                .build();
    }

    /**
     * 登出：清除当前请求的 Sa-Token 会话。
     */
    public void logout() {
        StpUtil.logout();
    }


    /**
     * 构建用户信息（vben5 /user/info 兼容结构）。
     * 权限聚合：user → user_role → role → role_menu → menu.perm。
     */
    public UserInfoVo getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectOneById(userId);
        List<String> roleCodes = getRoleCodes(userId);
        return UserInfoVo.builder()
                .userId(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roles(roleCodes)
                .permissions(getPermissionCodes(userId, roleCodes))
                .build();
    }

    /**
     * 查询用户角色编码集合（user → user_role → role）。
     */
    public List<String> getRoleCodes(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectListByQuery(
                QueryWrapper.create().where(USER_ID.eq(userId)));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        List<SysRole> roles = sysRoleMapper.selectListByQuery(
                QueryWrapper.create().where(new QueryColumn("id").in(roleIds)));
        return roles.stream().map(SysRole::getCode).toList();
    }

    /**
     * 查询用户权限标识集合（role → role_menu → menu.perm）。
     * 若角色含 super_admin，直接返回全量权限标记 *:*:*。
     */
    public List<String> getPermissionCodes(Long userId) {
        return getPermissionCodes(userId, getRoleCodes(userId));
    }

    private List<String> getPermissionCodes(Long userId, List<String> roleCodes) {
        // super_admin 返回菜单表全量权限码：Sa-Token 注解按字面匹配，
        // 前端 hasAccessByCodes 也是字面匹配，通配符 *:*:* 两边都不生效
        if (roleCodes.contains(SUPER_ADMIN)) {
            return sysMenuMapper.selectListByQuery(QueryWrapper.create())
                    .stream()
                    .map(SysMenu::getPerm)
                    .filter(perm -> perm != null && !perm.isBlank())
                    .distinct()
                    .toList();
        }
        Set<Long> roleIds = sysRoleMapper.selectListByQuery(
                        QueryWrapper.create().where(new QueryColumn("code").in(roleCodes)))
                .stream().map(SysRole::getId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> menuIds = sysRoleMenuMapper.selectListByQuery(
                        QueryWrapper.create().where(ROLE_ID.in(roleIds)))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysMenuMapper.selectListByQuery(
                        QueryWrapper.create().where(new QueryColumn("id").in(menuIds)))
                .stream()
                .map(SysMenu::getPerm)
                .filter(perm -> perm != null && !perm.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
