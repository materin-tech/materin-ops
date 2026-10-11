package com.materin.tech.ops.components.ticket.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.ticket.entity.Ticket;
import com.materin.tech.ops.components.ticket.entity.TicketAction;
import com.materin.tech.ops.components.ticket.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单管理接口（ITR + SLA）。
 */
@RestController
@RequestMapping("/ticket/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /** 条件分页 */
    @GetMapping
    public Result<PageResult<Ticket>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Integer priority,
                                           @RequestParam(required = false) Integer type,
                                           @RequestParam(required = false) Long assigneeId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(ticketService.page(keyword, status, priority, type, assigneeId, page, pageSize));
    }

    /** 工单详情 */
    @GetMapping("/{id}")
    public Result<Ticket> detail(@PathVariable Long id) {
        return Result.ok(ticketService.detail(id));
    }

    /** 操作流水 */
    @GetMapping("/{id}/actions")
    public Result<List<TicketAction>> actions(@PathVariable Long id) {
        return Result.ok(ticketService.actions(id));
    }

    /** 创建工单（报障对所有登录用户开放；可同时委派处理人） */
    @PostMapping
    public Result<Ticket> create(@Valid @RequestBody TicketBody body) {
        Ticket ticket = new Ticket();
        ticket.setTitle(body.getTitle());
        ticket.setDescription(body.getDescription());
        ticket.setType(body.getType());
        ticket.setPriority(body.getPriority());
        ticket.setAssigneeId(body.getAssigneeId());
        return Result.ok(ticketService.create(ticket));
    }

    /** 编辑工单 */
    @SaCheckPermission("ticket:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TicketBody body) {
        Ticket ticket = new Ticket();
        ticket.setTitle(body.getTitle());
        ticket.setDescription(body.getDescription());
        ticket.setType(body.getType());
        ticket.setPriority(body.getPriority());
        ticketService.update(id, ticket);
        return Result.ok();
    }

    /** 委派/改派处理人 */
    @SaCheckPermission("ticket:handle")
    @PutMapping("/{id}/delegate")
    public Result<Void> delegate(@PathVariable Long id, @Valid @RequestBody AssignBody body) {
        ticketService.assign(id, body.getAssigneeId());
        return Result.ok();
    }

    /** 删除工单 */
    @SaCheckPermission("ticket:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return Result.ok();
    }

    /** 受理 */
    @SaCheckPermission("ticket:handle")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id, @RequestBody(required = false) AssignBody body) {
        ticketService.accept(id, body == null ? null : body.getAssigneeId());
        return Result.ok();
    }

    /** 转派 */
    @SaCheckPermission("ticket:handle")
    @PutMapping("/{id}/assignee")
    public Result<Void> reassign(@PathVariable Long id, @Valid @RequestBody AssignBody body) {
        ticketService.reassign(id, body.getAssigneeId());
        return Result.ok();
    }

    /** 备注 */
    @SaCheckPermission("ticket:handle")
    @PutMapping("/{id}/comment")
    public Result<Void> comment(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
        ticketService.comment(id, body.getContent());
        return Result.ok();
    }

    /** 状态流转 */
    @SaCheckPermission("ticket:handle")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        ticketService.changeStatus(id, status);
        return Result.ok();
    }

    /** 关闭 */
    @SaCheckPermission("ticket:close")
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        ticketService.close(id);
        return Result.ok();
    }

    @Data
    public static class TicketBody {
        @NotBlank(message = "标题不能为空")
        private String title;
        private String description;
        private Integer type;
        private Integer priority;
        /** 创建即委派的处理人（可空） */
        private Long assigneeId;
    }

    @Data
    public static class AssignBody {
        private Long assigneeId;
    }

    @Data
    public static class CommentBody {
        @NotBlank(message = "内容不能为空")
        private String content;
    }
}
