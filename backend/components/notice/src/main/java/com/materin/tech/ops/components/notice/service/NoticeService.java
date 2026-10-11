package com.materin.tech.ops.components.notice.service;

import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.common.notice.NoticeMessage;
import com.materin.tech.ops.common.notice.NoticeSender;
import com.materin.tech.ops.components.notice.entity.Notice;
import com.materin.tech.ops.components.notice.mapper.NoticeMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知服务：实现 common 的 NoticeSender SPI，提供收件箱查询与已读管理。
 */
@Service
@RequiredArgsConstructor
public class NoticeService implements NoticeSender {

    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_RECEIVER_ID = new QueryColumn("receiver_id");
    private static final QueryColumn C_READ_FLAG = new QueryColumn("read_flag");

    private final NoticeMapper noticeMapper;

    /** 站内信之外的 IM 渠道广播（可选依赖，循环依赖安全：仅在发送接口触发） */
    private final ObjectProvider<NotifyChannelService> channelServiceProvider;

    @Override
    public void send(NoticeMessage message) {
        Notice notice = new Notice();
        notice.setTitle(message.getTitle());
        notice.setContent(message.getContent());
        notice.setType(message.getType() == null ? "system" : message.getType());
        notice.setLevel(message.getLevel() == null ? "info" : message.getLevel());
        notice.setReceiverId(message.getReceiverUserId());
        notice.setSenderId(message.getSenderId());
        notice.setLink(message.getLink());
        notice.setReadFlag(0);
        noticeMapper.insert(notice);
    }

    /** 当前登录用户 ID（APP 虚拟身份无收件箱） */
    private Long currentUserId() {
        String id = StpUtil.getLoginId().toString();
        if (id.startsWith("app:")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "APP 身份无收件箱");
        }
        return Long.valueOf(id);
    }

    /** 我的收件箱分页（含未读数） */
    public PageResult<Notice> myNotices(Integer readFlag, int page, int pageSize) {
        Long userId = currentUserId();
        QueryWrapper qw = QueryWrapper.create().where(C_RECEIVER_ID.eq(userId));
        if (readFlag != null) {
            qw.and(C_READ_FLAG.eq(readFlag));
        }
        qw.orderBy(C_ID.desc());
        Page<Notice> p = noticeMapper.paginate(Page.of(page, pageSize), qw);
        return PageResult.of(p.getRecords(), p.getTotalRow(), page, pageSize);
    }

    /** 当前用户未读数 */
    public long unreadCount() {
        Long userId = currentUserId();
        Long count = noticeMapper.selectCountByQuery(QueryWrapper.create()
                .where(C_RECEIVER_ID.eq(userId))
                .and(C_READ_FLAG.eq(0)));
        return count == null ? 0 : count;
    }

    /** 标记已读（仅收件人本人可操作） */
    public void markRead(Long id) {
        Notice notice = requireMine(id);
        if (notice.getReadFlag() != null && notice.getReadFlag() == 1) {
            return;
        }
        Notice upd = new Notice();
        upd.setId(id);
        upd.setReadFlag(1);
        upd.setReadAt(LocalDateTime.now());
        noticeMapper.update(upd);
    }

    /** 全部已读 */
    public void markAllRead() {
        Long userId = currentUserId();
        Notice upd = new Notice();
        upd.setReadFlag(1);
        upd.setReadAt(LocalDateTime.now());
        noticeMapper.updateByQuery(upd, QueryWrapper.create()
                .where(C_RECEIVER_ID.eq(userId))
                .and(C_READ_FLAG.eq(0)));
    }

    /** 删除我的通知 */
    public void remove(Long id) {
        requireMine(id);
        noticeMapper.deleteById(id);
    }

    private Notice requireMine(Long id) {
        Notice notice = noticeMapper.selectOneById(id);
        if (notice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        if (!notice.getReceiverId().equals(currentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作他人通知");
        }
        return notice;
    }

    /** 批量发送给多用户（管理端广播用）：站内信 + IM 渠道并行广播 */
    public void sendToUsers(List<Long> userIds, NoticeMessage template) {
        for (Long userId : userIds) {
            send(NoticeMessage.builder()
                    .title(template.getTitle())
                    .content(template.getContent())
                    .type(template.getType())
                    .level(template.getLevel())
                    .receiverUserId(userId)
                    .senderId(template.getSenderId())
                    .link(template.getLink())
                    .build());
        }
        // 广播到启用的 IM 渠道（企微/钉钉/飞书），失败仅记日志
        try {
            channelServiceProvider.ifAvailable(
                    svc -> svc.broadcastToIm(template.getTitle(),
                            template.getContent() == null ? template.getTitle() : template.getContent()));
        } catch (Exception e) {
            // 广播失败不影响站内信
        }
    }
}
