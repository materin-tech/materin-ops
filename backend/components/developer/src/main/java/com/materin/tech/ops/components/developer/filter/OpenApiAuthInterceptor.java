package com.materin.tech.ops.components.developer.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.materin.tech.ops.components.developer.entity.DevApi;
import com.materin.tech.ops.components.developer.entity.DevApp;
import com.materin.tech.ops.components.developer.entity.DevAppApi;
import com.materin.tech.ops.components.developer.mapper.DevApiMapper;
import com.materin.tech.ops.components.developer.mapper.DevAppApiMapper;
import com.materin.tech.ops.components.developer.mapper.DevAppMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 开放 API 签名鉴权拦截器（与登录体系并行的双通道）。
 * <p>请求头携带 X-App-Key 时走 APP 鉴权链路：
 * HMAC-SHA256 签名校验 → 时间窗口 + nonce 防重放 → 应用启用状态 →
 * 应用-接口授权匹配（method + path，且接口为开放状态）→ 通过后以
 * "app:{appKey}" 虚拟身份建立 Sa-Token 会话放行。
 * 不带 X-App-Key 的请求直接放行，交由 Sa-Token 登录链路处理。</p>
 */
@Component
@RequiredArgsConstructor
public class OpenApiAuthInterceptor implements HandlerInterceptor {

    public static final String APP_KEY_HEADER = "X-App-Key";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";
    public static final String NONCE_HEADER = "X-Nonce";
    public static final String SIGNATURE_HEADER = "X-Signature";

    /** 签名时间窗口（毫秒）：±5 分钟 */
    private static final long TIMESTAMP_WINDOW_MS = 5 * 60 * 1000L;
    /** nonce 防重放 Redis 键前缀与 TTL */
    private static final String NONCE_KEY_PREFIX = "dev:openapi:nonce:";
    private static final Duration NONCE_TTL = Duration.ofMinutes(10);

    private static final QueryColumn C_APP_KEY = new QueryColumn("app_key");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_APP_ID = new QueryColumn("app_id");
    private static final QueryColumn C_API_ID = new QueryColumn("api_id");
    private static final QueryColumn C_METHOD = new QueryColumn("method");
    private static final QueryColumn C_PATH = new QueryColumn("path");

    private final DevAppMapper devAppMapper;
    private final DevApiMapper devApiMapper;
    private final DevAppApiMapper devAppApiMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String appKey = request.getHeader(APP_KEY_HEADER);
        if (appKey == null || appKey.isBlank()) {
            // 非 APP 通道：交给 Sa-Token 登录链路
            return true;
        }
        try {
            verifyAndLogin(appKey, request);
            return true;
        } catch (OpenApiAuthException e) {
            response.setStatus(e.httpStatus);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    java.util.Map.of("code", e.httpStatus, "message", e.getMessage(), "data", "")));
            return false;
        }
    }

    /** 校验签名与授权，通过后建立 APP 虚拟会话 */
    private void verifyAndLogin(String appKey, HttpServletRequest request) {
        DevApp app = devAppMapper.selectOneByQuery(
                QueryWrapper.create().where(C_APP_KEY.eq(appKey)));
        if (app == null) {
            throw new OpenApiAuthException(401, "未知的应用 AppKey");
        }
        if (app.getStatus() == null || app.getStatus() != 1) {
            throw new OpenApiAuthException(403, "应用已被禁用");
        }

        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);
        if (timestamp == null || nonce == null || signature == null) {
            throw new OpenApiAuthException(401, "缺少签名请求头（X-Timestamp / X-Nonce / X-Signature）");
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new OpenApiAuthException(401, "X-Timestamp 格式非法");
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > TIMESTAMP_WINDOW_MS) {
            throw new OpenApiAuthException(401, "签名已过期（时间窗口 ±5 分钟）");
        }

        // nonce 防重放：SETNX 成功即首次使用
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(NONCE_KEY_PREFIX + appKey + ":" + nonce, "1", NONCE_TTL);
        if (first == null || !first) {
            throw new OpenApiAuthException(401, "nonce 已被使用，疑似重放请求");
        }

        String bodySha256;
        try {
            if (request instanceof RequestCachingFilter.CachedBodyRequest cached) {
                bodySha256 = sha256Hex(cached.getContentAsByteArray());
            } else {
                bodySha256 = sha256Hex("".getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new OpenApiAuthException(401, "读取请求体失败");
        }

        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();
        String signString = appKey + "\n" + method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + bodySha256;
        String expected = hmacSha256Hex(app.getAppSecret(), signString);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new OpenApiAuthException(401, "签名校验失败");
        }

        // 授权匹配：该应用已授权且接口处于开放状态
        DevApi api = devApiMapper.selectOneByQuery(QueryWrapper.create()
                .where(C_METHOD.eq(method))
                .and(C_PATH.eq(path))
                .and(C_STATUS.eq(1)));
        if (api == null) {
            throw new OpenApiAuthException(403, "接口不存在或未开放");
        }
        Long authCount = devAppApiMapper.selectCountByQuery(QueryWrapper.create()
                .where(C_APP_ID.eq(app.getId()))
                .and(C_API_ID.eq(api.getId())));
        if (authCount == null || authCount == 0) {
            throw new OpenApiAuthException(403, "应用未授权该接口");
        }

        // APP 虚拟会话：接口级白名单已由本拦截器控制，权限码全放行
        StpUtil.login("app:" + appKey);
    }

    public static String hmacSha256Hex(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    public static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    /** 鉴权失败异常（携带 HTTP 状态码） */
    static class OpenApiAuthException extends RuntimeException {
        final int httpStatus;

        OpenApiAuthException(int httpStatus, String message) {
            super(message);
            this.httpStatus = httpStatus;
        }
    }
}
