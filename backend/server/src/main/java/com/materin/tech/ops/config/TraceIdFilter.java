package com.materin.tech.ops.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 全链路追踪：优先透传上游 X-Trace-Id，否则生成；写入 MDC 供日志输出，并回写响应头。
 * 记录访问日志（路径/方法/状态/耗时），零业务侵入。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID);
        if (traceId == null || traceId.isBlank() || traceId.length() > 64) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID, traceId);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            if (log.isInfoEnabled()) {
                log.info("ACCESS {} {} -> {} ({}ms) from {}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), System.currentTimeMillis() - start,
                        request.getRemoteAddr());
            }
            MDC.remove(MDC_KEY);
        }
    }
}
