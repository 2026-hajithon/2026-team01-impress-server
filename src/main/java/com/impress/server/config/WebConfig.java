package com.impress.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 API 경로에 대해 적용
                .allowedOrigins("https://2026-team01-impress-web.vercel.app",
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://localhost:5174") // Vercel 도메인 (끝에 슬래시 / 없이 작성)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // JWT 토큰이나 쿠키 등 인증 정보를 포함한 요청을 허용
                .maxAge(3600); // Preflight(OPTIONS) 요청의 캐시 시간(초)
    }
}