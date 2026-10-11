package com.materin.tech.ops.components.notice.channel;

import com.materin.tech.ops.components.notice.entity.NotifyChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 企业微信群机器人：POST {webhook}?key=xxx，msgtype=markdown。
 */
@Component
public class WecomChannelSender implements ImChannelSender {

    @Override
    public String type() {
        return "wecom";
    }

    @Override
    public String send(NotifyChannel channel, String title, String markdownContent) throws Exception {
        String url = channel.getWebhookUrl();
        Map<String, Object> payload = Map.of(
                "msgtype", "markdown",
                "markdown", Map.of("content", "**" + title + "**\n" + markdownContent));
        String resp = RestClient.create().post()
                .uri(url)
                .body(payload)
                .retrieve()
                .body(String.class);
        return resp;
    }
}
