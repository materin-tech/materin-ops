package com.materin.tech.ops.components.notice.service;

import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.notice.channel.ImChannelSender;
import com.materin.tech.ops.components.notice.entity.NotifyChannel;
import com.materin.tech.ops.components.notice.mapper.NotifyChannelMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知渠道服务：渠道 CRUD、连通性测试、按启用渠道广播 IM 消息。
 */
@Slf4j
@Service
public class NotifyChannelService {

    private static final QueryColumn C_TYPE = new QueryColumn("type");
    private static final QueryColumn C_ENABLED = new QueryColumn("enabled");

    private final NotifyChannelMapper notifyChannelMapper;
    private final Map<String, ImChannelSender> senders;

    public NotifyChannelService(NotifyChannelMapper notifyChannelMapper,
                                List<ImChannelSender> senders) {
        this.notifyChannelMapper = notifyChannelMapper;
        this.senders = senders.stream()
                .collect(Collectors.toMap(ImChannelSender::type, Function.identity()));
    }

    /** 全量渠道列表 */
    public List<NotifyChannel> listAll() {
        return notifyChannelMapper.selectListByQuery(
                QueryWrapper.create().orderBy(new QueryColumn("id").asc()));
    }

    /** 保存（新增或编辑） */
    public void save(NotifyChannel body) {
        String type = body.getType() == null ? "" : body.getType();
        if (!senders.containsKey(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的渠道类型：" + type);
        }
        if (body.getWebhookUrl() == null || !body.getWebhookUrl().startsWith("http")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Webhook 地址非法");
        }
        if (body.getId() == null) {
            notifyChannelMapper.insert(body);
        } else {
            NotifyChannel exist = requireChannel(body.getId());
            exist.setName(body.getName());
            exist.setWebhookUrl(body.getWebhookUrl());
            exist.setSecret(body.getSecret());
            exist.setEnabled(body.getEnabled() == null ? 1 : body.getEnabled());
            notifyChannelMapper.update(exist);
            exist.setId(body.getId());
        }
    }

    /** 删除渠道（逻辑删除） */
    public void delete(Long id) {
        requireChannel(id);
        notifyChannelMapper.deleteById(id);
    }

    /**
     * 连通性测试：向渠道发送测试消息。
     */
    public String test(Long id) {
        NotifyChannel channel = requireChannel(id);
        ImChannelSender sender = senders.get(channel.getType());
        if (sender == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该渠道类型无发送实现");
        }
        try {
            return sender.send(channel, "【格物】渠道测试",
                    "这是一条渠道连通性测试消息，收到即代表配置成功。- " + java.time.LocalDateTime.now());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "渠道调用失败：" + e.getMessage());
        }
    }

    /**
     * 广播：站内信之外，把消息推送到所有启用的 IM 渠道（失败不影响主流程，仅记日志）。
     */
    public void broadcastToIm(String title, String markdown) {
        List<NotifyChannel> channels = notifyChannelMapper.selectListByQuery(
                QueryWrapper.create().where(C_ENABLED.eq(1)));
        for (NotifyChannel channel : channels) {
            ImChannelSender sender = senders.get(channel.getType());
            if (sender == null) {
                continue;
            }
            try {
                String resp = sender.send(channel, title, markdown);
                log.info("IM 渠道 [{}] 推送结果: {}", channel.getName(), resp);
            } catch (Exception e) {
                log.warn("IM 渠道 [{}] 推送失败: {}", channel.getName(), e.getMessage());
            }
        }
    }

    private NotifyChannel requireChannel(Long id) {
        NotifyChannel channel = notifyChannelMapper.selectOneById(id);
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "渠道不存在");
        }
        return channel;
    }
}
