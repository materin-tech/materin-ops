package com.materin.tech.ops.components.ticket.service;

import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.components.ticket.entity.SlaPolicy;
import com.materin.tech.ops.components.ticket.entity.Ticket;
import com.materin.tech.ops.components.ticket.mapper.SlaPolicyMapper;
import com.materin.tech.ops.components.ticket.mapper.TicketActionMapper;
import com.materin.tech.ops.components.ticket.mapper.TicketMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单服务单元测试：SLA 匹配 / 工单号防撞 / 状态机 / 编辑删除限制（TC-TKT-002/003/004/007/008/009）。
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketActionMapper ticketActionMapper;
    @Mock
    private SlaPolicyMapper slaPolicyMapper;

    private TicketService service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketMapper, ticketActionMapper, slaPolicyMapper);
        stpMock = mockStatic(StpUtil.class);
        stpMock.when(StpUtil::getLoginId).thenReturn("1");
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    private SlaPolicy sla(int priority, int respMinutes, int resvMinutes) {
        SlaPolicy p = new SlaPolicy();
        p.setId((long) (priority + 1));
        p.setName("P" + (priority + 1));
        p.setPriority(priority);
        p.setFirstResponseMinutes(respMinutes);
        p.setResolveMinutes(resvMinutes);
        p.setEnabled(1);
        return p;
    }

    @Test
    @DisplayName("TC-TKT-003: 创建 P1 工单按 SLA 匹配时限（30min/4h）")
    void create_appliesP1SlaDeadlines() {
        when(slaPolicyMapper.selectOneByQuery(any()))
                .thenReturn(sla(0, 30, 240));
        when(ticketMapper.selectMaxTicketNo(anyString())).thenReturn(null);

        Ticket body = new Ticket();
        body.setTitle("P1 故障");
        body.setPriority(0);
        LocalDateTime before = LocalDateTime.now();
        Ticket created = service.create(body);

        assertThat(created.getTicketNo()).isEqualTo("TK-20261011-0001");
        assertThat(created.getSlaPolicyId()).isEqualTo(1L);
        assertThat(created.getResponseDeadline()).isNotNull();
        assertThat(created.getResponseDeadline())
                .isAfterOrEqualTo(before.plusMinutes(30))
                .isBefore(LocalDateTime.now().plusMinutes(30).plusSeconds(5));
        assertThat(created.getResolveDeadline())
                .isAfterOrEqualTo(before.plusMinutes(240));
    }

    @Test
    @DisplayName("TC-TKT-002: 工单号取当日最大号+1（含已删除，防撞号）")
    void create_ticketNoIncrementsFromMax() {
        when(slaPolicyMapper.selectOneByQuery(any())).thenReturn(null);
        when(ticketMapper.selectMaxTicketNo(anyString())).thenReturn("TK-20261011-0007");

        Ticket body = new Ticket();
        body.setTitle("撞号回归");
        body.setPriority(2);
        Ticket created = service.create(body);

        assertThat(created.getTicketNo()).isEqualTo("TK-20261011-0008");
    }

    @Test
    @DisplayName("TC-TKT-006: 受理写入首次响应时间并改派")
    void accept_setsFirstResponseAndAssignee() {
        Ticket existing = new Ticket();
        existing.setId(9L);
        existing.setStatus(0);
        when(ticketMapper.selectOneById(9L)).thenReturn(existing);

        service.accept(9L, 5L);

        verify(ticketMapper).update(any(Ticket.class));
    }

    @Test
    @DisplayName("TC-TKT-008: 非法流转 0→3 拒绝")
    void changeStatus_illegalTransitionRejected() {
        Ticket existing = new Ticket();
        existing.setId(1L);
        existing.setStatus(0);
        when(ticketMapper.selectOneById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> service.changeStatus(1L, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许的状态变更");
    }

    @Test
    @DisplayName("TC-TKT-007: 合法流转 1→2 通过")
    void changeStatus_legalTransitionPasses() {
        Ticket existing = new Ticket();
        existing.setId(1L);
        existing.setStatus(1);
        when(ticketMapper.selectOneById(1L)).thenReturn(existing);

        service.changeStatus(1L, 2);

        verify(ticketMapper).update(any(Ticket.class));
    }

    @Test
    @DisplayName("TC-TKT-009: 编辑已解决工单拒绝")
    void update_resolvedTicketRejected() {
        Ticket existing = new Ticket();
        existing.setId(2L);
        existing.setStatus(3);
        when(ticketMapper.selectOneById(2L)).thenReturn(existing);

        Ticket body = new Ticket();
        body.setTitle("改标题");
        assertThatThrownBy(() -> service.update(2L, body))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可编辑");
    }

    @Test
    @DisplayName("TC-TKT-010: 删除工单同时清理流水")
    void delete_removesActions() {
        Ticket existing = new Ticket();
        existing.setId(3L);
        existing.setStatus(1);
        when(ticketMapper.selectOneById(3L)).thenReturn(existing);

        service.delete(3L);

        verify(ticketMapper).deleteById(3L);
        verify(ticketActionMapper).deleteByQuery(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("委派待受理单记为首次响应")
    void assign_pendingTicketSetsFirstResponse() {
        Ticket existing = new Ticket();
        existing.setId(4L);
        existing.setStatus(0);
        when(ticketMapper.selectOneById(4L)).thenReturn(existing);

        service.assign(4L, 7L);

        verify(ticketMapper).update(any(Ticket.class));
    }
}
