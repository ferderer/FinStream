package pro.finstream.broadcasting.common.error;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the FinStream Broadcasting Service.
 * 
 * Handles service-specific exceptions that may not be covered by the standard
 * Spring Boot error handling mechanism, particularly for WebSocket and Kafka operations.
 * 
 * Banking Industry Pattern: Comprehensive exception handling for financial
 * data streaming with proper error logging and client-friendly responses.
 * 
 * @author FinStream Platform
 * @version 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles BaseException instances with detailed error context.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Map<String, Object>> handleBaseException(BaseException ex) {
        log.error("BaseException occurred: {} - {}", ex.getCode(), ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ex.getCode().name(),
            ex.getMessage(),
            ex.getStatus()
        );
        
        if (!ex.getData().isEmpty()) {
            response.put("data", ex.getData());
        }
        
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * Handles JWT validation exceptions.
     */
    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<Map<String, Object>> handleJwtValidationException(JwtValidationException ex) {
        log.error("JWT validation failed: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ErrorCode.E_INVALID_JWT_TOKEN.name(),
            "JWT token validation failed",
            HttpStatus.UNAUTHORIZED
        );
        
        response.put("data", Map.of(
            "jwtError", ex.getMessage(),
            "advice", "Please obtain a new JWT token from the SSO service"
        ));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles authentication failures.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("Authentication failed: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ErrorCode.E_ACCESS_DENIED.name(),
            "Authentication failed",
            HttpStatus.UNAUTHORIZED
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles WebSocket specific exceptions.
    @ExceptionHandler(WebSocketException.class)
    public ResponseEntity<Map<String, Object>> handleWebSocketException(WebSocketException ex) {
        log.error("WebSocket error occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ErrorCode.E_WEBSOCKET_CONNECTION_FAILED.name(),
            "WebSocket communication error",
            HttpStatus.BAD_REQUEST
        );
        
        response.put("data", Map.of(
            "webSocketError", ex.getMessage(),
            "advice", "Check WebSocket connection and try reconnecting"
        ));
        
        return ResponseEntity.badRequest().body(response);
    }
     */

    /**
     * Handles messaging delivery exceptions.
     */
    @ExceptionHandler(MessageDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleMessageDeliveryException(MessageDeliveryException ex) {
        log.error("Message delivery failed: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ErrorCode.E_MESSAGE_BROADCAST_FAILED.name(),
            "Failed to deliver message to client",
            HttpStatus.SERVICE_UNAVAILABLE
        );
        
        response.put("data", Map.of(
            "deliveryError", ex.getMessage(),
            "advice", "Message will be retried automatically"
        ));
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles general messaging exceptions.
     */
    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<Map<String, Object>> handleMessagingException(MessagingException ex) {
        log.error("Messaging error occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ErrorCode.E_WEBSOCKET_MESSAGE_INVALID.name(),
            "Message processing error",
            HttpStatus.BAD_REQUEST
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles Kafka exceptions.
     */
    @ExceptionHandler({KafkaException.class, org.apache.kafka.common.KafkaException.class})
    public ResponseEntity<Map<String, Object>> handleKafkaException(Exception ex) {
        log.error("Kafka error occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ErrorCode.E_KAFKA_CONSUMER_ERROR.name(),
            "Kafka messaging system error",
            HttpStatus.SERVICE_UNAVAILABLE
        );
        
        response.put("data", Map.of(
            "kafkaError", ex.getMessage(),
            "impact", "Real-time stock updates may be delayed",
            "advice", "System will attempt automatic recovery"
        ));
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles Kafka listener execution failures.
     */
    @ExceptionHandler(ListenerExecutionFailedException.class)
    public ResponseEntity<Map<String, Object>> handleListenerExecutionFailedException(ListenerExecutionFailedException ex) {
        log.error("Kafka listener execution failed: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ErrorCode.E_KAFKA_CONSUMER_ERROR.name(),
            "Kafka message processing failed",
            HttpStatus.SERVICE_UNAVAILABLE
        );
        
        response.put("data", Map.of(
            "listenerError", ex.getMessage(),
            "partition", ex.getGroupId(),
            "advice", "Message processing will be retried"
        ));
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            ErrorCode.E_INTERNAL_ERROR.name(),
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        response.put("data", Map.of(
            "errorClass", ex.getClass().getSimpleName(),
            "advice", "Please contact system administrator if this persists"
        ));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Creates a standardized error response map.
     */
    private Map<String, Object> createErrorResponse(String code, String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("status", status.value());
        response.put("timestamp", java.time.Instant.now());
        response.put("service", "finstream-broadcasting-service");
        return response;
    }
}
