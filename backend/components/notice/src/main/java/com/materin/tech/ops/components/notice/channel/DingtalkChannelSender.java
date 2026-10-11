package com.materin.tech.ops.components.notice.channel;

import com.materin.tech.ops.components.notice.entity.NotifyChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 钉钉群机器人：POST {webhook}?access_token=xxx，msgtype=markdown。
 * 安全设置为「加签」时：sign = urlencode(base64(hmacSHA256(ts + "\n" + secret, secret)))，
 * 追加 &timestamp=ms&sign=xxx。
 */
@Component
public class DingtalkChannelSender implements ImChannelSender {

    @Override
    public String type() {
        return "dingtalk";
    }

    @Override
    public String send(NotifyChannel channel, String title, String markdownContent) throws Exception {
        String url = channel.getWebhookUrl();
        if (channel.getSecret() != null && !channel.getSecret().isBlank()) {
            long ts = System.currentTimeMillis();
            String stringToSign = ts + "\n" + channel.getSecret();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(channel.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sign = Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
            url = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("timestamp", ts)
                    .queryParam("sign", sign)
                    .encode()
                    .toUriString();
        }
        Map<String, Object> payload = Map.of(
                "msgtype", "markdown",
                "markdown", Map.of("title", title, "text", "**" + title + "**\n" + markdownContent));
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
