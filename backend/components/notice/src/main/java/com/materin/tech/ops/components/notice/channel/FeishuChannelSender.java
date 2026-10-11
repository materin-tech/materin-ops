package com.materin.tech.ops.components.notice.channel;

import com.materin.tech.ops.components.notice.entity.NotifyChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 飞书自定义机器人：POST {webhook}，msg_type=text（富文本卡片用 interactive，此处以文本保兼容）。
 */
@Component
public class FeishuChannelSender implements ImChannelSender {

    @Override
    public String type() {
        return "feishu";
    }

    @Override
    public String send(NotifyChannel channel, String title, String markdownContent) throws Exception {
        // 飞书 text 消息不支持 markdown 渲染，剥离常见标记保证可读
        String plain = markdownContent
                .replace("**", "")
                .replace("###", "")
                .replaceAll("\\n{2,}", "\n");
        Map<String, Object> payload = Map.of(
                "msg_type", "text",
                "content", Map.of("text", "【" + title + "】\n" + plain));
        return post(channel.getWebhookUrl(), payload);
    }

    private static final java.net.http.HttpClient HTTP_CLIENT =
            java.net.http.HttpClient.newHttpClient();

    private String post(String url, Map<String, Object> payload) throws Exception {
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        java.net.http.HttpResponse<String> resp =
                HTTP_CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }
}
