package com.org.devgenie.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.devgenie.dto.coverage.ProgressUpdate;
import com.org.devgenie.service.coverage.SessionManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CoverageProgressWebSocketHandler implements WebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session.getUri());
        sessions.put(sessionId, session);
        log.info("WebSocket connection established for session: {}", sessionId);
        
        // Send initial connection confirmation
        ProgressUpdate initialUpdate = ProgressUpdate.builder()
                .sessionId(sessionId)
                .progress(0.0)
                .currentStep("Connected")
                .message("WebSocket connection established")
                .type(ProgressUpdate.ProgressType.INITIALIZATION)
                .build();
        
        sendProgressUpdate(sessionId, initialUpdate);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // Handle incoming messages if needed (e.g., cancel requests)
        log.debug("Received WebSocket message: {}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = extractSessionId(session.getUri());
        log.error("WebSocket transport error for session: {}", sessionId, exception);
        sessions.remove(sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = extractSessionId(session.getUri());
        sessions.remove(sessionId);
        log.info("WebSocket connection closed for session: {}", sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Send progress update to specific session
     */
    public void sendProgressUpdate(String sessionId, ProgressUpdate update) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(update);
                session.sendMessage(new TextMessage(json));
                log.debug("Sent progress update to session {}: {}%", sessionId, update.getProgress());
            } catch (IOException e) {
                log.error("Failed to send progress update to session: {}", sessionId, e);
            }
        }
    }

    /**
     * Event listener for progress updates from SessionManagementService
     */
    @EventListener
    public void handleProgressUpdateEvent(SessionManagementService.ProgressUpdateEvent event) {
        ProgressUpdate update = event.getProgressUpdate();
        sendProgressUpdate(update.getSessionId(), update);
    }

    /**
     * Extract session ID from WebSocket URI query parameters
     */
    private String extractSessionId(URI uri) {
        if (uri == null) return "unknown";
        
        String query = uri.getQuery();
        log.debug("Extracting session ID from WebSocket query: {}", query);
        
        if (query != null && query.contains("sessionId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("sessionId=")) {
                    String sessionId = param.substring("sessionId=".length());
                    log.debug("Extracted session ID: {} from query: {}", sessionId, query);
                    return sessionId;
                }
            }
        }
        
        // Fallback: try to extract from path if query parameter not found
        String path = uri.getPath();
        if (path.contains("/")) {
            String[] segments = path.split("/");
            for (String segment : segments) {
                if (segment.length() > 30 && segment.contains("-")) {
                    // Looks like a UUID session ID
                    log.debug("Extracted session ID from path: {} from URI: {}", segment, uri);
                    return segment;
                }
            }
        }
        
        log.warn("Unable to extract session ID from URI: {}", uri);
        return "unknown";
    }

    /**
     * Get active session count (for monitoring)
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Broadcast message to all connected sessions (if needed)
     */
    public void broadcastMessage(String message) {
        TextMessage textMessage = new TextMessage(message);
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Failed to broadcast message to session", e);
                }
            }
        });
    }
}
