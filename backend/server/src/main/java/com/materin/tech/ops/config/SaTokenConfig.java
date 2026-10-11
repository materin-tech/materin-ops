package com.materin.tech.ops.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.components.developer.filter.OpenApiAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Sa-Token 配置：双通道鉴权。
 * <p>第一通道：开放 API 签名拦截器（请求头带 X-App-Key 时验签并建立 APP 虚拟会话）；
 * 第二通道：Sa-Token 登录拦截器（其余请求要求登录）。
 * knife4j 文档路径（/doc.html、/v3/api-docs）放行。</p>
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private static final List<String> OPEN_PATHS = List.of("/auth/login", "/doc.html", "/v3/api-docs/**", "/webjars/**", "/favicon.ico");

    private final OpenApiAuthInterceptor openApiAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 通道一：开放 API 签名（不带 X-App-Key 时直接放行给通道二）
        registry.addInterceptor(openApiAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(OPEN_PATHS)
                .order(0);
        // 通道二：登录校验
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(OPEN_PATHS)
                .order(1);
    }
}
