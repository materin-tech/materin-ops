package com.materin.tech.ops.components.ops.requirement.service;

import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.ops.requirement.entity.ProductMember;
import com.materin.tech.ops.components.ops.requirement.entity.ProjectProduct;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductMemberMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProjectProductMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 需求模块数据权限：以产品为授权单元。
 * <p>super_admin 返回 null 表示不做过滤；普通用户按 product_member 授权集合过滤。
 * project/iteration/task 经 project_product 由可见产品推导可见项目，再按 project_id 过滤。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductDataScopeService {

    public static final String ROLE_SUPER_ADMIN = "super_admin";

    private static final QueryColumn C_USER_ID = new QueryColumn("user_id");
    private static final QueryColumn C_PRODUCT_ID = new QueryColumn("product_id");
    private static final QueryColumn C_PROJECT_ID = new QueryColumn("project_id");

    private final ProductMemberMapper productMemberMapper;
    private final ProjectProductMapper projectProductMapper;

    /** 当前登录用户 ID；开放 API 的 APP 虚拟身份返回 null */
    public Long currentUserId() {
        String id = StpUtil.getLoginId().toString();
        return id.startsWith("app:") ? null : Long.valueOf(id);
    }

    /** 当前用户可见产品 ID 集合；返回 null 表示不过滤（super_admin 或开放 API 的 APP 虚拟身份） */
    public Set<Long> visibleProductIds(Long userId) {
        // APP 通道（loginId 为 "app:appKey"）：接口白名单已控制访问面，跳过用户级数据权限
        if (userId == null) {
            return null;
        }
        if (StpUtil.hasRole(ROLE_SUPER_ADMIN)) {
            return null;
        }
        return productMemberMapper.selectListByQuery(
                        QueryWrapper.create().select(C_PRODUCT_ID).where(C_USER_ID.eq(userId)))
                .stream()
                .map(ProductMember::getProductId)
                .collect(Collectors.toSet());
    }

    /** 当前用户可见项目 ID 集合（经 project_product 推导）；返回 null 表示 super_admin 不过滤 */
    public Set<Long> visibleProjectIds(Long userId) {
        Set<Long> productIds = visibleProductIds(userId);
        if (productIds == null) {
            return null;
        }
        if (productIds.isEmpty()) {
            return Set.of();
        }
        return projectProductMapper.selectListByQuery(
                        QueryWrapper.create().select(C_PROJECT_ID).where(C_PRODUCT_ID.in(productIds)))
                .stream()
                .map(ProjectProduct::getProjectId)
                .collect(Collectors.toSet());
    }

    /** 断言产品对当前用户可见，越权抛业务异常 */
    public void assertProductVisible(Long userId, Long productId) {
        if (userId == null) {
            return;
        }
        Set<Long> ids = visibleProductIds(userId);
        if (ids != null && !ids.contains(productId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该产品");
        }
    }

    /** 断言项目对当前用户可见，越权抛业务异常 */
    public void assertProjectVisible(Long userId, Long projectId) {
        if (userId == null) {
            return;
        }
        Set<Long> ids = visibleProjectIds(userId);
        if (ids != null && !ids.contains(projectId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该项目");
        }
    }
}
