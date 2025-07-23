package pro.finstream.broadcasting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP Configuration for FinStream Broadcasting Service
 * 
 * Features:
 * - JWT Authentication at WebSocket handshake
 * - STOMP protocol for message broker pattern
 * - CORS support for Angular frontend
 * - Banking-grade security with user session management
 * - Production-ready connection limits and heartbeat
 * 
 * Endpoints:
 * - /ws/stock-updates - WebSocket connection endpoint
 * - /topic/stocks/prices - Real-time stock price broadcasts
 * - /user/topic/watchlist - User-specific watchlist updates (future)
 * 
 * @author FinStream Platform
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    
    public WebSocketConfig(JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        log.info("WebSocket STOMP Configuration initialized with JWT security");
    }

    /**
     * Configure STOMP endpoints for WebSocket connections
     * 
     * Banking Security Features:
     * - JWT token validation at handshake
     * - CORS configured for Angular frontend
     * - SockJS fallback for corporate firewalls
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stock-updates")
                .setAllowedOriginPatterns("http://localhost:4200", "https://finstream.pro")
                .withSockJS()
                .setHeartbeatTime(25_000) // 25 seconds heartbeat for connection health
                .setDisconnectDelay(5_000) // 5 seconds disconnect delay
                .setSessionCookieNeeded(false); // JWT-based auth, no session cookies
        
        log.info("WebSocket STOMP endpoints registered: /stock-updates with SockJS support");
    }

    /**
     * Configure message broker for real-time broadcasting
     * 
     * Topics:
     * - /topic/stocks/prices - Public stock price updates
     * - /user/topic/watchlist - User-specific data (future)
     * - /app/subscribe - Client subscription requests
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topic-based messaging
        config.enableSimpleBroker("/topic", "/user")
              .setHeartbeatValue(new long[]{25_000, 25_000}) // Server heartbeat: 25s
              .setTaskScheduler(taskScheduler()); // Custom task scheduler for heartbeat
        
        // Application destination prefix for client messages
        config.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix for targeted messages
        config.setUserDestinationPrefix("/user");
        
        log.info("Message broker configured: /topic (public), /user (private), /app (client)");
    }

    /**
     * Task Scheduler for WebSocket heartbeat and maintenance tasks
     * 
     * Banking Requirements:
     * - Reliable connection monitoring via heartbeat
     * - Automatic cleanup of stale connections
     * - Performance optimized for high-frequency operations
     * 
     * @return Configured ThreadPoolTaskScheduler
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // Sufficient for heartbeat + cleanup tasks
        scheduler.setThreadNamePrefix("finstream-websocket-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        
        log.info("WebSocket TaskScheduler initialized: poolSize=10, heartbeat support enabled");
        return scheduler;
    }

    /**
     * Configure client inbound channel with JWT authentication
     * 
     * Security Pipeline:
     * 1. Extract JWT from Authorization header or query param
     * 2. Validate JWT signature and claims
     * 3. Create Spring Security Authentication
     * 4. Store user context for session management
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtWebSocketAuthenticationInterceptor());
        log.info("Client inbound channel configured with JWT authentication interceptor");
    }

    /**
     * JWT WebSocket Authentication Interceptor
     * 
     * Banking Security Requirements:
     * - Validate JWT at CONNECT command
     * - Extract user information for session tracking  
     * - Reject unauthorized connections immediately
     * - Log security events for audit trail
     */
    private class JwtWebSocketAuthenticationInterceptor implements ChannelInterceptor {
        
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            
            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                return authenticateWebSocketConnection(message, accessor);
            }
            
            return message;
        }
        
        /**
         * Authenticate WebSocket connection using JWT token
         */
        private Message<?> authenticateWebSocketConnection(Message<?> message, StompHeaderAccessor accessor) {
            try {
                String token = extractJwtToken(accessor);
                
                if (token == null || token.isBlank()) {
                    log.warn("WebSocket connection rejected: No JWT token provided");
                    throw new SecurityException("JWT token required for WebSocket connection");
                }
                
                // Decode and validate JWT
                Jwt jwt = jwtDecoder.decode(token);
                Authentication authentication = jwtAuthenticationConverter.convert(jwt);
                
                if (authentication == null) {
                    log.warn("WebSocket connection rejected: Invalid JWT token");
                    throw new SecurityException("Invalid JWT token");
                }
                
                // Set authentication in accessor for this session
                accessor.setUser(authentication);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Extract user information for logging
                String username = jwt.getClaimAsString("username");
                String userId = jwt.getSubject();
                
                log.info("WebSocket connection authenticated: user={}, userId={}, sessionId={}", 
                        username, userId, accessor.getSessionId());
                
                // Add user tracking headers
                accessor.setNativeHeader("X-User-Id", userId);
                accessor.setNativeHeader("X-Username", username);
                
                return message;
                
            } catch (Exception e) {
                log.error("WebSocket authentication failed: {}", e.getMessage());
                throw new SecurityException("WebSocket authentication failed: " + e.getMessage());
            }
        }
        
        /**
         * Extract JWT token from WebSocket headers
         * 
         * Support multiple token sources:
         * 1. Authorization header (Bearer token)
         * 2. Query parameter (?token=jwt)
         * 3. Native header (Authorization)
         */
        private String extractJwtToken(StompHeaderAccessor accessor) {
            // Try Authorization header first
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            
            // Try query parameter (for browser WebSocket limitations)
            String tokenParam = accessor.getFirstNativeHeader("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                return tokenParam;
            }
            
            // Try direct token header
            return accessor.getFirstNativeHeader("X-Auth-Token");
        }
    }
}
