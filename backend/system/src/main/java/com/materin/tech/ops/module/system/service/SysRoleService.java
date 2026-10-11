package com.materin.tech.ops.module.system.service;

import com.materin.tech.ops.common.api.PageParam;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.module.system.dto.RoleCreateBody;
import com.materin.tech.ops.module.system.dto.RoleUpdateBody;
import com.materin.tech.ops.module.system.vo.RoleVo;
import com.materin.tech.ops.module.system.entity.SysRole;
import com.materin.tech.ops.module.system.entity.SysRoleMenu;
import com.materin.tech.ops.module.system.mapper.SysRoleMapper;
import com.materin.tech.ops.module.system.mapper.SysRoleMenuMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统角色服务：分页 / 列表 / 增删改 / 分配菜单。
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_NAME = new QueryColumn("name");
    private static final QueryColumn C_CODE = new QueryColumn("code");
    private static final QueryColumn C_SORT = new QueryColumn("sort");
    private static final QueryColumn C_ROLE_ID = new QueryColumn("role_id");

    /**
     * 条件分页查询角色（name/code 模糊匹配）。
     */
    public PageResult<RoleVo> pageRoles(String name, String code, PageParam param) {
        QueryWrapper qw = QueryWrapper.create();
        if (name != null && !name.isBlank()) {
            qw.where(C_NAME.like(name));
        }
        if (code != null && !code.isBlank()) {
            qw.and(C_CODE.like(code));
        }
        qw.orderBy(C_SORT.asc(), C_ID.asc());
        Page<SysRole> p = sysRoleMapper.paginate(Page.of(param.safePage(), param.safePageSize()), qw);
        List<RoleVo> vos = p.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(vos, p.getTotalRow(), param.safePage(), param.safePageSize());
    }

    /**
     * 全量角色列表（下拉框用，不分页）。
     */
    public List<RoleVo> listAll() {
        return sysRoleMapper.selectListByQuery(
                QueryWrapper.create().orderBy(C_SORT.asc(), C_ID.asc()))
                .stream().map(this::toVo).toList();
    }

    /**
     * 新增角色：code 唯一校验 + 保存菜单绑定。
     */
    @Transactional
    public Long createRole(RoleCreateBody body) {
        Long count = sysRoleMapper.selectCountByQuery(
                QueryWrapper.create().where(C_CODE.eq(body.getCode())));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setName(body.getName());
        role.setCode(body.getCode());
        role.setSort(body.getSort() == null ? 0 : body.getSort());
        role.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        role.setRemark(body.getRemark());
        sysRoleMapper.insert(role);
        if (body.getMenuIds() != null && !body.getMenuIds().isEmpty()) {
            assignMenus(role.getId(), body.getMenuIds());
        }
        return role.getId();
    }

    /**
     * 编辑角色：更新基础字段；menuIds 非空时全量覆盖菜单绑定。
     */
    @Transactional
    public void updateRole(Long id, RoleUpdateBody body) {
        SysRole role = sysRoleMapper.selectOneById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        // code 修改时需唯一性校验（排除自身）
        if (body.getCode() != null && !body.getCode().equals(role.getCode())) {
            Long count = sysRoleMapper.selectCountByQuery(
                    QueryWrapper.create().where(C_CODE.eq(body.getCode())));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "角色编码已存在");
            }
        }
        if (body.getName() != null) role.setName(body.getName());
        if (body.getCode() != null) role.setCode(body.getCode());
        if (body.getSort() != null) role.setSort(body.getSort());
        if (body.getStatus() != null) role.setStatus(body.getStatus());
        if (body.getRemark() != null) role.setRemark(body.getRemark());
        sysRoleMapper.update(role);
        if (body.getMenuIds() != null && !body.getMenuIds().isEmpty()) {
            assignMenus(id, body.getMenuIds());
        }
    }

    /**
     * 分配菜单：先清空角色现有绑定，再批量插入。
     */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.deleteByQuery(
                QueryWrapper.create().where(C_ROLE_ID.eq(roleId)));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<SysRoleMenu> list = menuIds.stream()
                .map(menuId -> {
                    SysRoleMenu rm = new SysRoleMenu();
                    rm.setRoleId(roleId);
                    rm.setMenuId(menuId);
                    return rm;
                })
                .toList();
        sysRoleMenuMapper.insertBatch(list);
    }

    /**
     * 删除角色：逻辑删除 + 清空菜单绑定；super_admin 不允许删除。
     */
    @Transactional
    public void deleteRole(Long id) {
        SysRole role = sysRoleMapper.selectOneById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        // 超级管理员角色是系统内置角色，不允许删除
        if (AuthService.SUPER_ADMIN.equals(role.getCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "超级管理员角色不允许删除");
        }
        sysRoleMapper.deleteById(id);
        sysRoleMenuMapper.deleteByQuery(
                QueryWrapper.create().where(C_ROLE_ID.eq(id)));
    }

    /** 角色转视图：附带菜单绑定 ID 集合 */
    private RoleVo toVo(SysRole role) {
        RoleVo vo = new RoleVo();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setCode(role.getCode());
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setMenuIds(sysRoleMenuMapper.selectListByQuery(
                        QueryWrapper.create().where(C_ROLE_ID.eq(role.getId())))
                .stream().map(SysRoleMenu::getMenuId).toList());
        return vo;
    }
}
