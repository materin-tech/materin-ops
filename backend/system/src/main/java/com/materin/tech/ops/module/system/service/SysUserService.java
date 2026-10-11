package com.materin.tech.ops.module.system.service;

import cn.dev33.satoken.secure.BCrypt;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.module.system.dto.UserCreateBody;
import com.materin.tech.ops.module.system.dto.UserUpdateBody;
import com.materin.tech.ops.module.system.dto.UserPageQuery;
import com.materin.tech.ops.module.system.vo.UserVo;
import com.materin.tech.ops.module.system.entity.SysRole;
import com.materin.tech.ops.module.system.entity.SysUser;
import com.materin.tech.ops.module.system.entity.SysUserRole;
import com.materin.tech.ops.module.system.mapper.SysRoleMapper;
import com.materin.tech.ops.module.system.mapper.SysUserMapper;
import com.materin.tech.ops.module.system.mapper.SysUserRoleMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户服务：分页查询 / 增删改 / 重置密码 / 启禁用。
 * <p>写操作使用事务；绑定表操作为"先删后插"全量覆盖。</p>
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    // ---------- 表列常量（不依赖 APT 生成的 TableDef） ----------
    private static final QueryColumn C_USERNAME = new QueryColumn("username");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_USER_ID = new QueryColumn("user_id");
    private static final QueryColumn C_ROLE_ID = new QueryColumn("role_id");

    /**
     * 条件分页查询用户（username 模糊 / status 过滤），并聚合每个用户的角色信息。
     */
    public PageResult<UserVo> pageUsers(UserPageQuery query) {
        QueryWrapper qw = QueryWrapper.create();
        if (query.getUsername() != null && !query.getUsername().isBlank()) {
            qw.where(C_USERNAME.like(query.getUsername()));
        }
        if (query.getStatus() != null) {
            qw.and(C_STATUS.eq(query.getStatus()));
        }
        Page<SysUser> p = sysUserMapper.paginate(Page.of(query.safePage(), query.safePageSize()), qw);
        List<UserVo> vos = p.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(vos, p.getTotalRow(), query.safePage(), query.safePageSize());
    }

    /** 分页行转视图：聚合 roleIds 与 roleNames。 */
    private UserVo toVo(SysUser user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        List<SysUserRole> userRoles = sysUserRoleMapper.selectListByQuery(
                QueryWrapper.create().where(C_USER_ID.eq(user.getId())));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        vo.setRoleIds(roleIds);
        if (roleIds.isEmpty()) {
            vo.setRoleNames(Collections.emptyList());
        } else {
            vo.setRoleNames(sysRoleMapper.selectListByQuery(
                            QueryWrapper.create().where(new QueryColumn("id").in(roleIds)))
                    .stream().map(SysRole::getName).toList());
        }
        return vo;
    }

    /**
     * 新增用户：username 唯一校验 + BCrypt 加密落库 + 绑定角色。
     */
    @Transactional
    public void createUser(UserCreateBody body) {
        Long count = sysUserMapper.selectCountByQuery(
                QueryWrapper.create().where(C_USERNAME.eq(body.getUsername())));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(body.getUsername());
        user.setPassword(BCrypt.hashpw(body.getPassword(), BCrypt.gensalt()));
        user.setNickname(body.getNickname());
        user.setEmail(body.getEmail());
        user.setPhone(body.getPhone());
        user.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        sysUserMapper.insert(user);
        bindRoles(user.getId(), body.getRoleIds());
    }

    /**
     * 编辑用户：更新基础字段并全量重绑角色。
     */
    @Transactional
    public void updateUser(Long id, UserUpdateBody body) {
        SysUser user = sysUserMapper.selectOneById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (body.getNickname() != null) user.setNickname(body.getNickname());
        if (body.getEmail() != null) user.setEmail(body.getEmail());
        if (body.getPhone() != null) user.setPhone(body.getPhone());
        if (body.getStatus() != null) user.setStatus(body.getStatus());
        sysUserMapper.update(user);
        // 全量覆盖式重绑角色：先清空再插入
        bindRoles(id, body.getRoleIds());
    }

    /**
     * 重置密码：BCrypt 加密后更新。
     */
    public void resetPassword(Long id, String newPassword) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        sysUserMapper.update(user);
    }

    /**
     * 启用/禁用用户。
     */
    public void changeStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        sysUserMapper.update(user);
    }

    /**
     * 删除用户：逻辑删除用户并物理删除角色绑定。
     */
    @Transactional
    public void deleteUser(Long id) {
        sysUserMapper.deleteById(id);
        sysUserRoleMapper.deleteByQuery(
                QueryWrapper.create().where(C_USER_ID.eq(id)));
    }

    /** 重绑角色：先物理删除旧绑定，再批量插入新绑定（roleIds 为空则仅清空） */
    private void bindRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.deleteByQuery(QueryWrapper.create().where(C_USER_ID.eq(userId)));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysUserRole> list = roleIds.stream()
                .map(roleId -> {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    return ur;
                })
                .toList();
        sysUserRoleMapper.insertBatch(list);
    }
}
