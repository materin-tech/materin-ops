package com.materin.tech.ops.components.notice.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.common.notice.NoticeMessage;
import com.materin.tech.ops.components.notice.entity.Notice;
import com.materin.tech.ops.components.notice.entity.NotifyChannel;
import com.materin.tech.ops.components.notice.service.NoticeChannelService;
import com.materin.tech.ops.components.notice.service.NoticeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
 * 站内通知接口：我的收件箱 / 已读管理 / 通知发送。
 */
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
@Validated
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeChannelService notifyChannelService;

    /** 我的收件箱分页 */
    @GetMapping("/my")
    public Result<PageResult<Notice>> my(@RequestParam(required = false) Integer readFlag,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(noticeService.myNotices(readFlag, page, pageSize));
    }

    /** 未读数 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(noticeService.unreadCount());
    }

    /** 标记已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        noticeService.markRead(id);
        return Result.ok();
    }

    /** 全部已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        noticeService.markAllRead();
        return Result.ok();
    }

    /** 删除我的通知 */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        noticeService.remove(id);
        return Result.ok();
    }

    /** 发送通知（广播给指定用户） */
    @SaCheckPermission("notice:send")
    @PostMapping("/send")
    public Result<Void> send(@Valid @RequestBody SendBody body) {
        NoticeMessage message = NoticeMessage.builder()
                .title(body.getTitle())
                .content(body.getContent())
                .type(body.getType())
                .level(body.getLevel())
                .senderId(StpUtil.getLoginIdAsLong())
                .link(body.getLink())
                .build();
        noticeService.sendToUsers(body.getUserIds(), message);
        return Result.ok();
    }

    // ---------- 通知渠道（企微/钉钉/飞书） ----------

    /** 渠道列表 */
    @GetMapping("/channels")
    public Result<List<NotifyChannel>> channels() {
        return Result.ok(notifyChannelService.listAll());
    }

    /** 保存渠道（新增或编辑） */
    @SaCheckPermission("notice:channel:edit")
    @PostMapping("/channels")
    public Result<Void> saveChannel(@RequestBody NotifyChannel body) {
        notifyChannelService.save(body);
        return Result.ok();
    }

    /** 删除渠道 */
    @SaCheckPermission("notice:channel:edit")
    @DeleteMapping("/channels/{id}")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        notifyChannelService.delete(id);
        return Result.ok();
    }

    /** 渠道连通性测试 */
    @SaCheckPermission("notice:channel:test")
    @PostMapping("/channels/{id}/test")
    public Result<String> testChannel(@PathVariable Long id) {
        return Result.ok(notifyChannelService.test(id));
    }

    /** 通知发送请求体 */
    @Data
    public static class SendBody {

        @NotBlank(message = "标题不能为空")
        private String title;

        private String content;

        /** 类型：system / announcement / todo */
        private String type;

        /** 级别：info / warn / error */
        private String level;

        /** 点击跳转路由（可空） */
        private String link;

        @NotEmpty(message = "接收人不能为空")
        private List<Long> userIds;
    }
}
