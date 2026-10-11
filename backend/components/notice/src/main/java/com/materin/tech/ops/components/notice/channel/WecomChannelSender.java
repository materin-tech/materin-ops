package com.materin.tech.ops.components.notice.channel;

import com.materin.tech.ops.components.notice.entity.NotifyChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
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
        return post(url, payload);
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
