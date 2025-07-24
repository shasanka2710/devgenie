package com.org.devgenie.config;

import com.org.devgenie.websocket.CoverageProgressWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private CoverageProgressWebSocketHandler coverageProgressHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(coverageProgressHandler, "/ws/coverage-progress")
                .setAllowedOrigins("*"); // Configure based on your security requirements
        // No SockJS for now to avoid path pattern conflicts
    }
}
