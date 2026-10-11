package com.materin.tech.ops.components.ticket.service;

import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.ticket.entity.SlaPolicy;
import com.materin.tech.ops.components.ticket.entity.Ticket;
import com.materin.tech.ops.components.ticket.entity.TicketAction;
import com.materin.tech.ops.components.ticket.mapper.SlaPolicyMapper;
import com.materin.tech.ops.components.ticket.mapper.TicketActionMapper;
import com.materin.tech.ops.components.ticket.mapper.TicketMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单服务：创建（SLA 匹配计时）→ 受理 → 处理 → 待确认 → 解决 → 关闭，全程留痕。
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_TICKET_NO = new QueryColumn("ticket_no");
    private static final QueryColumn C_TITLE = new QueryColumn("title");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_PRIORITY = new QueryColumn("priority");
    private static final QueryColumn C_TYPE = new QueryColumn("type");
    private static final QueryColumn C_REPORTER_ID = new QueryColumn("reporter_id");
    private static final QueryColumn C_ASSIGNEE_ID = new QueryColumn("assignee_id");
    private static final QueryColumn C_TICKET_ID = new QueryColumn("ticket_id");
    private static final QueryColumn C_PRIORITY_SLA = new QueryColumn("priority");
    private static final QueryColumn C_ENABLED = new QueryColumn("enabled");

    private final TicketMapper ticketMapper;
    private final TicketActionMapper ticketActionMapper;
    private final SlaPolicyMapper slaPolicyMapper;

    /** 创建工单：生成工单号 + 按 SLA 计算时限 */
    @Transactional
    public Ticket create(Ticket body) {
        Long userId = currentUserId();
        Ticket ticket = new Ticket();
        ticket.setTitle(body.getTitle());
        ticket.setDescription(body.getDescription());
        ticket.setType(body.getType() == null ? 0 : body.getType());
        ticket.setPriority(body.getPriority() == null ? 2 : body.getPriority());
        ticket.setStatus(0);
        ticket.setReporterId(userId);
        ticket.setAssigneeId(body.getAssigneeId());
        ticket.setTicketNo(nextTicketNo());
        // SLA 匹配：按优先级取启用中的策略
        SlaPolicy sla = slaPolicyMapper.selectOneByQuery(QueryWrapper.create()
                .where(C_PRIORITY_SLA.eq(ticket.getPriority()))
                .and(C_ENABLED.eq(1)));
        if (sla != null) {
            LocalDateTime now = LocalDateTime.now();
            ticket.setSlaPolicyId(sla.getId());
            ticket.setResponseDeadline(now.plusMinutes(sla.getFirstResponseMinutes()));
            ticket.setResolveDeadline(now.plusMinutes(sla.getResolveMinutes()));
        }
        ticketMapper.insert(ticket);
        log(userId, ticket.getId(), 0, "创建工单");
        return ticket;
    }

    /** 受理：待受理 → 处理中，记录首次响应时间 */
    @Transactional
    public void accept(Long id, Long assigneeId) {
        Ticket ticket = requireTicket(id);
        if (ticket.getStatus() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待受理工单可受理");
        }
        Ticket upd = new Ticket();
        upd.setId(id);
        upd.setStatus(1);
        upd.setAssigneeId(assigneeId != null ? assigneeId : currentUserId());
        upd.setFirstResponseAt(LocalDateTime.now());
        ticketMapper.update(upd);
        log(currentUserId(), id, 1, "受理工单");
    }

    /** 转派 */
    @Transactional
    public void reassign(Long id, Long assigneeId) {
        requireTicket(id);
        Ticket upd = new Ticket();
        upd.setId(id);
        upd.setAssigneeId(assigneeId);
        ticketMapper.update(upd);
        log(currentUserId(), id, 2, "转派给用户 #" + assigneeId);
    }

    /** 备注 */
    @Transactional
    public void comment(Long id, String content) {
        requireTicket(id);
        log(currentUserId(), id, 3, content);
    }

    /** 编辑工单：标题/描述/类型/优先级；优先级变更时按 SLA 重算时限 */
    @Transactional
    public void update(Long id, Ticket body) {
        Ticket ticket = requireTicket(id);
        if (ticket.getStatus() >= 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已解决/已关闭工单不可编辑");
        }
        Ticket upd = new Ticket();
        upd.setId(id);
        if (body.getTitle() != null) upd.setTitle(body.getTitle());
        if (body.getDescription() != null) upd.setDescription(body.getDescription());
        if (body.getType() != null) upd.setType(body.getType());
        boolean priorityChanged = body.getPriority() != null && !body.getPriority().equals(ticket.getPriority());
        if (priorityChanged) {
            upd.setPriority(body.getPriority());
            SlaPolicy sla = slaPolicyMapper.selectOneByQuery(QueryWrapper.create()
                    .where(C_PRIORITY_SLA.eq(body.getPriority()))
                    .and(C_ENABLED.eq(1)));
            LocalDateTime base = LocalDateTime.now();
            if (sla != null) {
                upd.setSlaPolicyId(sla.getId());
                if (ticket.getFirstResponseAt() == null) {
                    upd.setResponseDeadline(base.plusMinutes(sla.getFirstResponseMinutes()));
                }
                upd.setResolveDeadline(base.plusMinutes(sla.getResolveMinutes()));
            }
        }
        ticketMapper.update(upd);
        log(currentUserId(), id, 3, "编辑工单" + (priorityChanged ? "（优先级变更，SLA 已重算）" : ""));
    }

    /** 删除工单（逻辑删除）并清理操作流水 */
    @Transactional
    public void delete(Long id) {
        requireTicket(id);
        ticketMapper.deleteById(id);
        ticketActionMapper.deleteByQuery(QueryWrapper.create().where(C_TICKET_ID.eq(id)));
    }

    /** 委派/改派（受理前后均可指派处理人） */
    @Transactional
    public void assign(Long id, Long assigneeId) {
        Ticket ticket = requireTicket(id);
        Ticket upd = new Ticket();
        upd.setId(id);
        upd.setAssigneeId(assigneeId);
        if (ticket.getStatus() == 0 && ticket.getFirstResponseAt() == null) {
            upd.setFirstResponseAt(LocalDateTime.now());
        }
        ticketMapper.update(upd);
        log(currentUserId(), id, 2, "委派给用户 #" + assigneeId);
    }

    /** 状态流转（0待受理→1处理中→2待确认→3已解决→4已关闭；受理走 accept） */
    @Transactional
    public void changeStatus(Long id, Integer target) {
        Ticket ticket = requireTicket(id);
        int from = ticket.getStatus();
        boolean legal = (from == 1 && target == 2)
                || (from == 2 && target == 1)
                || (from == 2 && target == 3)
                || (from == 3 && target == 2);
        if (!legal) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不允许的状态变更：" + from + " → " + target);
        }
        Ticket upd = new Ticket();
        upd.setId(id);
        upd.setStatus(target);
        if (target == 3) {
            upd.setResolvedAt(LocalDateTime.now());
        }
        ticketMapper.update(upd);
        log(currentUserId(), id, 4, "状态 " + from + " → " + target);
    }

    /** 关闭工单（受理后任意状态可关） */
    @Transactional
    public void close(Long id) {
        Ticket ticket = requireTicket(id);
        if (ticket.getStatus() == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待受理工单请先受理或直接删除");
        }
        if (ticket.getStatus() == 4) {
            return;
        }
        Ticket upd = new Ticket();
        upd.setId(id);
        upd.setStatus(4);
        upd.setClosedAt(LocalDateTime.now());
        ticketMapper.update(upd);
        log(currentUserId(), id, 6, "关闭工单");
    }

    /** 条件分页（含 SLA 超期标志） */
    public PageResult<Ticket> page(String keyword, Integer status, Integer priority, Integer type,
                                   Long assigneeId, int pageNo, int pageSize) {
        QueryWrapper qw = QueryWrapper.create();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(C_TITLE.like(keyword).or(C_TICKET_NO.like(keyword)));
        }
        if (status != null) {
            qw.and(C_STATUS.eq(status));
        }
        if (priority != null) {
            qw.and(C_PRIORITY.eq(priority));
        }
        if (type != null) {
            qw.and(C_TYPE.eq(type));
        }
        if (assigneeId != null) {
            qw.and(C_ASSIGNEE_ID.eq(assigneeId));
        }
        qw.orderBy(C_ID.desc());
        Page<Ticket> p = ticketMapper.paginate(Page.of(pageNo, pageSize), qw);
        return PageResult.of(p.getRecords(), p.getTotalRow(), pageNo, pageSize);
    }

    /** 工单详情 + 操作流水 */
    public Ticket detail(Long id) {
        return requireTicket(id);
    }

    public List<TicketAction> actions(Long id) {
        requireTicket(id);
        return ticketActionMapper.selectListByQuery(
                QueryWrapper.create().where(C_TICKET_ID.eq(id)).orderBy(C_ID.asc()));
    }

    /** SLA 是否超期（解决/关闭前，now 超过对应 deadline） */
    public boolean slaBreached(Ticket t) {
        LocalDateTime now = LocalDateTime.now();
        if (t.getStatus() >= 3) {
            return t.getResolveDeadline() != null && t.getResolvedAt() != null
                    && t.getResolvedAt().isAfter(t.getResolveDeadline());
        }
        if (t.getStatus() == 0) {
            return t.getResponseDeadline() != null && now.isAfter(t.getResponseDeadline());
        }
        return t.getResolveDeadline() != null && now.isAfter(t.getResolveDeadline());
    }

    /** 工单号：TK-yyyyMMdd-4 位流水（按当日最大号 +1，含已删除工单，避免唯一键冲突） */
    private String nextTicketNo() {
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TK-" + date + "-";
        String maxNo = ticketMapper.selectMaxTicketNo(prefix);
        int next = 1;
        if (maxNo != null && maxNo.length() > prefix.length()) {
            next = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%04d", next);
    }

    private void log(Long userId, Long ticketId, int action, String content) {
        TicketAction a = new TicketAction();
        a.setTicketId(ticketId);
        a.setUserId(userId);
        a.setAction(action);
        a.setContent(content);
        ticketActionMapper.insert(a);
    }

    private Ticket requireTicket(Long id) {
        Ticket ticket = ticketMapper.selectOneById(id);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    private Long currentUserId() {
        String id = StpUtil.getLoginId().toString();
        if (id.startsWith("app:")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "APP 身份不支持工单操作");
        }
        return Long.valueOf(id);
    }
}
