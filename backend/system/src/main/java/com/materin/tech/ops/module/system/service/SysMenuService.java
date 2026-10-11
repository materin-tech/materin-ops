package com.materin.tech.ops.module.system.service;

import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.module.system.dto.MenuBody;
import com.materin.tech.ops.module.system.vo.MenuTreeVo;
import com.materin.tech.ops.module.system.vo.RouteVo;
import com.materin.tech.ops.module.system.entity.SysMenu;
import com.materin.tech.ops.module.system.entity.SysRole;
import com.materin.tech.ops.module.system.entity.SysRoleMenu;
import com.materin.tech.ops.module.system.mapper.SysMenuMapper;
import com.materin.tech.ops.module.system.mapper.SysRoleMapper;
import com.materin.tech.ops.module.system.mapper.SysRoleMenuMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统菜单服务：菜单树 / 增删改 / 动态路由。
 */
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final AuthService authService;

    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_PARENT_ID = new QueryColumn("parent_id");
    private static final QueryColumn C_TYPE = new QueryColumn("type");
    private static final QueryColumn C_VISIBLE = new QueryColumn("visible");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_SORT = new QueryColumn("sort");
    private static final QueryColumn C_ROLE_ID = new QueryColumn("role_id");

    /** 菜单类型常量 */
    private static final String TYPE_DIR = "D";
    private static final String TYPE_MENU = "M";

    /**
     * 全量菜单树（管理端菜单管理使用，含按钮节点）。
     */
    public List<MenuTreeVo> getMenuTree() {
        List<SysMenu> all = sysMenuMapper.selectListByQuery(
                QueryWrapper.create().orderBy(C_SORT.asc(), C_ID.asc()));
        return buildMenuTree(all, 0L);
    }

    /**
     * 全量菜单平铺列表。
     */
    public List<MenuTreeVo> listAll() {
        return sysMenuMapper.selectListByQuery(
                QueryWrapper.create().orderBy(C_SORT.asc(), C_ID.asc()))
                .stream().map(this::toMenuTreeVo).toList();
    }

    /**
     * 新增菜单。
     */
    public Long createMenu(MenuBody body) {
        SysMenu menu = new SysMenu();
        applyBody(menu, body);
        sysMenuMapper.insert(menu);
        return menu.getId();
    }

    /**
     * 编辑菜单：不允许将 parent_id 指向自身。
     */
    public void updateMenu(Long id, MenuBody body) {
        SysMenu menu = sysMenuMapper.selectOneById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        if (body.getParentId() != null && body.getParentId().equals(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "父级菜单不能是自己");
        }
        applyBody(menu, body);
        sysMenuMapper.update(menu);
    }

    /**
     * 删除菜单：存在子菜单时禁止删除。
     */
    @Transactional
    public void deleteMenu(Long id) {
        Long childCount = sysMenuMapper.selectCountByQuery(
                QueryWrapper.create().where(C_PARENT_ID.eq(id)));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "存在子菜单，无法删除");
        }
        sysMenuMapper.deleteById(id);
    }


    /**
     * 当前用户可见的动态路由（后端动态路由模式）。
     * super_admin 全量可见，普通角色按 user→role→role_menu 过滤。
     */
    public List<RouteVo> getRoutes(Long userId) {
        List<SysMenu> menus;
        List<String> roleCodes = authService.getRoleCodes(userId);
        if (roleCodes.contains(AuthService.SUPER_ADMIN)) {
            menus = sysMenuMapper.selectListByQuery(routeQuery());
        } else {
            Set<Long> roleIds = sysRoleMapper.selectListByQuery(
                    QueryWrapper.create().where(new QueryColumn("code").in(roleCodes)))
                    .stream().map(SysRole::getId).collect(Collectors.toSet());
            if (roleIds.isEmpty()) {
                return Collections.emptyList();
            }
            Set<Long> menuIds = sysRoleMenuMapper.selectListByQuery(
                    QueryWrapper.create().where(C_ROLE_ID.in(roleIds)))
                    .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
            if (menuIds.isEmpty()) {
                return Collections.emptyList();
            }
            menus = sysMenuMapper.selectListByQuery(
                    routeQuery().and(new QueryColumn("id").in(menuIds)));
        }
        // 按 parentId 归组构建路由树
        List<RouteVo> tree = buildRouteTree(menus, 0L);
        return tree;
    }

    /** 路由菜单通用查询：type in (D, M) 且 visible=1 且 status=1 */
    private QueryWrapper routeQuery() {
        return QueryWrapper.create()
                .where(C_TYPE.in(List.of(TYPE_DIR, TYPE_MENU)))
                .and(C_VISIBLE.eq(1))
                .and(C_STATUS.eq(1))
                .orderBy(C_SORT.asc(), C_ID.asc());
    }

    /** 递归构建路由树（按 parentId 分组） */
    private List<RouteVo> buildRouteTree(List<SysMenu> menus, Long parentId) {
        List<RouteVo> result = new ArrayList<>();
        for (SysMenu m : menus) {
            if (!parentId.equals(m.getParentId())) {
                continue;
            }
            RouteVo vo = new RouteVo();
            vo.setName(routeName(m));
            vo.setPath(m.getPath());
            vo.setComponent(m.getComponent());
            RouteVo.Meta meta = new RouteVo.Meta();
            meta.setTitle(m.getName());
            meta.setIcon(m.getIcon());
            meta.setOrder(m.getSort());
            vo.setMeta(meta);
            vo.setChildren(buildRouteTree(menus, m.getId()));
            result.add(vo);
        }
        return result;
    }

    /** 由 path 推导路由 name，如 /system/users → SystemUsers */
    private String routeName(SysMenu m) {
        if (m.getPath() == null || m.getPath().isBlank()) {
            return "Menu" + m.getId();
        }
        String clean = m.getPath().replaceAll("^/+", "");
        if (clean.isBlank()) {
            return "Menu" + m.getId();
        }
        StringBuilder sb = new StringBuilder();
        for (String part : clean.split("/")) {
            if (part.isBlank()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1));
        }
        return sb.toString();
    }

    /** 递归构建完整菜单树（含按钮） */
    private List<MenuTreeVo> buildMenuTree(List<SysMenu> all, Long parentId) {
        List<MenuTreeVo> result = new ArrayList<>();
        for (SysMenu m : all) {
            if (!parentId.equals(m.getParentId())) {
                continue;
            }
            MenuTreeVo vo = toMenuTreeVo(m);
            vo.setChildren(buildMenuTree(all, m.getId()));
            result.add(vo);
        }
        return result;
    }

    /** 实体转菜单树节点 */
    private MenuTreeVo toMenuTreeVo(SysMenu m) {
        MenuTreeVo vo = new MenuTreeVo();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setName(m.getName());
        vo.setPath(m.getPath());
        vo.setComponent(m.getComponent());
        vo.setType(m.getType());
        vo.setPerm(m.getPerm());
        vo.setIcon(m.getIcon());
        vo.setSort(m.getSort());
        vo.setVisible(m.getVisible());
        vo.setStatus(m.getStatus());
        return vo;
    }

    /** 将请求体字段应用到实体（null 字段不覆盖已有值） */
    private void applyBody(SysMenu menu, MenuBody body) {
        if (body.getParentId() == null) menu.setParentId(0L);
        else menu.setParentId(body.getParentId());
        menu.setName(body.getName());
        menu.setPath(body.getPath());
        menu.setComponent(body.getComponent());
        menu.setType(body.getType());
        menu.setPerm(body.getPerm());
        menu.setIcon(body.getIcon());
        if (body.getSort() == null) menu.setSort(0);
        else menu.setSort(body.getSort());
        if (body.getVisible() == null) menu.setVisible(1);
        else menu.setVisible(body.getVisible());
        if (body.getStatus() == null) menu.setStatus(1);
        else menu.setStatus(body.getStatus());
    }
}
