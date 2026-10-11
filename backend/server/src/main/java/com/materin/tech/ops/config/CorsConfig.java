package com.materin.tech.ops.config;

import cn.dev33.satoken.filter.SaTokenCorsFilterForJakartaServlet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.servlet.Filter;

/**
 * CORS 统一配置：
 * 1. 禁用 Sa-Token 自带 CORS 过滤器（其 Origin 白名单校验导致 H5 等新端点报 Invalid CORS request）；
 * 2. 注册全局放行的 CORS 过滤器（最高优先级，先于一切业务过滤器写响应头）。
 */
@Configuration
public class CorsConfig {

    /** 禁用 Sa-Token 自带 CORS 过滤器：引用同 filter bean 并 setEnabled(false)，阻止其自动注册 */
    @Bean
    public FilterRegistrationBean<Filter> disableSaTokenCorsFilter(
            SaTokenCorsFilterForJakartaServlet saTokenCorsFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(saTokenCorsFilter);
        registration.setEnabled(false);
        return registration;
    }

    /** 全局 CORS 放行 */
    @Bean
    public FilterRegistrationBean<CorsFilter> globalCorsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
