package com.materin.tech.ops.components.ops.requirement.service;

import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.components.ops.requirement.entity.Story;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductModuleMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.StoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 需求状态机单元测试（TC-REQ-003）。
 */
@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock
    private StoryMapper storyMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductModuleMapper productModuleMapper;
    @Mock
    private ProductDataScopeService dataScopeService;
    @Mock
    private UserNicknameResolver nicknameResolver;

    private StoryService service() {
        return new StoryService(storyMapper, productMapper, productModuleMapper,
                dataScopeService, nicknameResolver);
    }

    @Test
    @DisplayName("TC-REQ-003a: 需求 0→1 提交评审通过")
    void changeStatus_draftToReviewing() {
        Story story = new Story();
        story.setId(1L);
        story.setProductId(10L);
        story.setStatus(0);
        when(storyMapper.selectOneById(1L)).thenReturn(story);
        when(dataScopeService.currentUserId()).thenReturn(1L);

        service().changeStatus(1L, 1);

        verify(storyMapper).update(any(Story.class));
    }

    @Test
    @DisplayName("TC-REQ-003b: 需求 0→3 直接关闭拒绝")
    void changeStatus_draftToClosedRejected() {
        Story story = new Story();
        story.setId(1L);
        story.setProductId(10L);
        story.setStatus(0);
        when(storyMapper.selectOneById(1L)).thenReturn(story);
        when(dataScopeService.currentUserId()).thenReturn(1L);

        assertThatThrownBy(() -> service().changeStatus(1L, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许的状态变更");
    }

    @Test
    @DisplayName("TC-REQ-003c: 需求 1→0 评审打回草稿通过")
    void changeStatus_reviewingBackToDraft() {
        Story story = new Story();
        story.setId(2L);
        story.setProductId(10L);
        story.setStatus(1);
        when(storyMapper.selectOneById(2L)).thenReturn(story);
        when(dataScopeService.currentUserId()).thenReturn(1L);

        service().changeStatus(2L, 0);

        verify(storyMapper).update(any(Story.class));
    }
}
