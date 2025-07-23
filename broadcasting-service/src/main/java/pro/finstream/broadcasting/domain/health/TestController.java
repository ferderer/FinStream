package pro.finstream.broadcasting.domain.health;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pro.finstream.broadcasting.domain.stockprice.StockPriceConsumerService;

/**
 * Test controller for the FinStream Broadcasting Service.
 * 
 * Provides simple endpoints to verify service startup, security configuration,
 * and JWT authentication without requiring full WebSocket or Kafka setup.
 * 
 * Banking Industry Pattern: Health check and diagnostic endpoints for
 * financial services monitoring and integration testing.
 * 
 * @author FinStream Platform
 * @version 1.0.0
 */
@RestController
@RequestMapping("/ws-test")
@RequiredArgsConstructor
public class TestController {

    @Value("${spring.application.name}")
    private String applicationName;
    
    @Value("${server.port}")
    private String serverPort;

    private final StockPriceConsumerService consumerService;
    
    @GetMapping("/kafka-status")
    public ResponseEntity<String> kafkaStatus() {
        return ResponseEntity.ok("Consumer service available: " + (consumerService != null));
    }

    /**
     * Public health check endpoint - no authentication required.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", applicationName);
        response.put("status", "UP");
        response.put("port", serverPort);
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "FinStream Broadcasting Service is running");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Public configuration test endpoint - shows CORS and basic setup.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", applicationName);
        response.put("contextPath", "/ws");
        response.put("corsEnabled", true);
        response.put("securityEnabled", true);
        response.put("timestamp", LocalDateTime.now());
        
        // Add security context info (if available)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        response.put("authenticated", auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Protected endpoint to test JWT authentication.
     */
    @GetMapping("/auth-test")
    public ResponseEntity<Map<String, Object>> authTest() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", applicationName);
        response.put("authenticated", true);
        response.put("timestamp", LocalDateTime.now());
        
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            response.put("subject", jwt.getSubject());
            response.put("username", jwt.getClaim("username"));
            response.put("email", jwt.getClaim("email"));
            response.put("issuer", jwt.getIssuer());
            response.put("issuedAt", jwt.getIssuedAt());
            response.put("expiresAt", jwt.getExpiresAt());
            response.put("scopes", jwt.getClaim("scope"));
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to simulate a broadcasting service operation.
     */
    @PostMapping("/broadcast-test")
    public ResponseEntity<Map<String, Object>> broadcastTest(@RequestBody(required = false) Map<String, Object> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", applicationName);
        response.put("operation", "broadcast-test");
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("timestamp", LocalDateTime.now());
        
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            response.put("userId", jwt.getSubject());
            response.put("username", jwt.getClaim("username"));
        }
        
        if (payload != null && !payload.isEmpty()) {
            response.put("receivedPayload", payload);
        }
        
        response.put("message", "Broadcast test completed successfully");
        response.put("status", "SUCCESS");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to test error handling.
     */
    @GetMapping("/error-test")
    public ResponseEntity<Map<String, Object>> errorTest(@RequestParam(defaultValue = "false") boolean throwError) {
        if (throwError) {
            throw new RuntimeException("Test error for error handling verification");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", applicationName);
        response.put("errorHandlingSetup", true);
        response.put("message", "Use ?throwError=true to test error handling");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Simple ping endpoint for connectivity testing.
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    /**
     * Service info endpoint with detailed information.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", applicationName);
        response.put("version", "1.0.0");
        response.put("port", serverPort);
        response.put("contextPath", "/ws");
        response.put("profile", "development");
        response.put("timestamp", LocalDateTime.now());
        
        // Add feature flags
        Map<String, Boolean> features = new HashMap<>();
        features.put("websocketEnabled", true);
        features.put("kafkaIntegrationEnabled", true);
        features.put("jwtAuthenticationEnabled", true);
        features.put("corsEnabled", true);
        features.put("actuatorEnabled", true);
        response.put("features", features);
        
        // Add endpoint information
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/ws-test/health");
        endpoints.put("config", "/ws-test/config");
        endpoints.put("authTest", "/ws-test/auth-test (requires JWT)");
        endpoints.put("broadcastTest", "/ws-test/broadcast-test (requires JWT)");
        endpoints.put("errorTest", "/ws-test/error-test");
        endpoints.put("actuatorHealth", "/actuator/health");
        response.put("endpoints", endpoints);
        
        return ResponseEntity.ok(response);
    }
}
