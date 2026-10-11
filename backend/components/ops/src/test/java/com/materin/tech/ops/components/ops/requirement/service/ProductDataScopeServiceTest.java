package com.materin.tech.ops.components.ops.requirement.service;

import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductMemberMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProjectProductMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 数据权限单元测试（TC-REQ-009 / TC-DEV 通道隔离）。
 */
@ExtendWith(MockitoExtension.class)
class ProductDataScopeServiceTest {

    @Mock
    private ProductMemberMapper productMemberMapper;
    @Mock
    private ProjectProductMapper projectProductMapper;

    private ProductDataScopeService service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new ProductDataScopeService(productMemberMapper, projectProductMapper);
        stpMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    @DisplayName("super_admin 不过滤（visibleProductIds 返回 null）")
    void superAdmin_skipsFilter() {
        stpMock.when(StpUtil::getLoginId).thenReturn("1");
        stpMock.when(() -> StpUtil.hasRole("super_admin")).thenReturn(true);

        assertThat(service.visibleProductIds(1L)).isNull();
    }

    @Test
    @DisplayName("APP 通道（app: 前缀）跳过用户级数据权限")
    void appPrincipal_skipsFilter() {
        stpMock.when(StpUtil::getLoginId).thenReturn("app:ak_test");

        assertThat(service.visibleProductIds(null)).isNull();
    }

    @Test
    @DisplayName("普通用户仅见授权产品集合")
    void normalUser_seesOnlyGrantedProducts() {
        stpMock.when(StpUtil::getLoginId).thenReturn("1");
        stpMock.when(() -> StpUtil.hasRole("super_admin")).thenReturn(false);
        com.materin.tech.ops.components.ops.requirement.entity.ProductMember m =
                new com.materin.tech.ops.components.ops.requirement.entity.ProductMember();
        m.setProductId(11L);
        when(productMemberMapper.selectListByQuery(any())).thenReturn(List.of(m));

        Set<Long> visible = service.visibleProductIds(1L);

        assertThat(visible).containsExactly(11L);
    }

    @Test
    @DisplayName("越权访问产品抛 403")
    void assertProductVisible_forbidden() {
        stpMock.when(StpUtil::getLoginId).thenReturn("1");
        stpMock.when(() -> StpUtil.hasRole("super_admin")).thenReturn(false);
        when(productMemberMapper.selectListByQuery(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.assertProductVisible(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问");
    }
}
