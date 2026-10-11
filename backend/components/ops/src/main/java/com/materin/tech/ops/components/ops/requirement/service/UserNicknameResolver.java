package com.materin.tech.ops.components.ops.requirement.service;

import com.materin.tech.ops.module.system.entity.SysUser;
import com.materin.tech.ops.module.system.mapper.SysUserMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户昵称解析器：批量把用户 ID 解析为展示名（昵称为空回退用户名）。
 */
@Component
@RequiredArgsConstructor
public class UserNicknameResolver {

    private final SysUserMapper sysUserMapper;

    /** 批量解析 userId → 展示名映射 */
    public Map<Long, String> resolve(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectListByQuery(
                        QueryWrapper.create().where(new QueryColumn("id").in(userIds)))
                .stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getNickname() == null || u.getNickname().isBlank() ? u.getUsername() : u.getNickname(),
                        (a, b) -> a));
    }

    /** 单个解析，查不到返回 null */
    public String resolveOne(Long userId) {
        if (userId == null) {
            return null;
        }
        Map<Long, String> map = resolve(Collections.singletonList(userId));
        return map.get(userId);
    }
}
