package com.impress.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 웹소켓 서버에 연결할 때 사용할 엔드포인트 설정 (명세서 3.1)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "https://2026-team01-impress-web.vercel.app", // 배포된 프론트엔드 도메인
                        "http://localhost:3000",                      // 로컬 테스트 (React/Next.js 기본 포트)
                        "http://localhost:5173"                       // 로컬 테스트 (Vite 기본 포트)
                );
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트로 메시지를 보낼 때 (SUB) 사용할 prefix (명세서 3.2, 3.3)
        // /topic: 방 전체 방송용
        // /queue, /user: 개인 에러 메시지 등 특정 유저 대상 1:1 전송용
        registry.enableSimpleBroker("/topic", "/queue", "/user");

        // 클라이언트 -> 서버로 메시지를 보낼 때 (PUB) 사용할 prefix (명세서 4.1, 5.2 등)
        registry.setApplicationDestinationPrefixes("/app");

        // 특정 사용자(User)에게 메시지를 보낼 때 경로를 설정하는 prefix
        registry.setUserDestinationPrefix("/user");
    }
}