package pro.finstream.broadcasting.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.KafkaException;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

/**
 * Custom error attributes for the FinStream Broadcasting Service.
 * 
 * Handles WebSocket, Kafka, JWT, and real-time communication specific errors
 * while maintaining consistency with the SSO module's error handling patterns.
 * 
 * Banking Industry Pattern: Comprehensive error mapping for financial
 * data streaming and real-time communication systems.
 * 
 * @author FinStream Platform
 * @version 1.0.0
 */
@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {
    
    private static final String ATTR_VALIDATIONS = "invalid";
    private static final String ATTR_MESSAGE_CODE = "code";
    
    private static final Map<Class<? extends Throwable>, HttpStatus> STATUS_MAP = new HashMap<>();
    
    static {
        // Standard Spring exceptions
        STATUS_MAP.put(java.nio.file.AccessDeniedException.class, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(BadCredentialsException.class, HttpStatus.UNAUTHORIZED);
        STATUS_MAP.put(ConstraintViolationException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(org.springframework.http.converter.HttpMessageNotReadableException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(InsufficientAuthenticationException.class, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(MethodArgumentNotValidException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(org.springframework.beans.TypeMismatchException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(org.springframework.security.core.userdetails.UsernameNotFoundException.class, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(UnsupportedOperationException.class, HttpStatus.NOT_IMPLEMENTED);
        
        // JWT and OAuth2 exceptions
        STATUS_MAP.put(JwtValidationException.class, HttpStatus.UNAUTHORIZED);
        STATUS_MAP.put(org.springframework.security.oauth2.jwt.JwtException.class, HttpStatus.UNAUTHORIZED);
        STATUS_MAP.put(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class, HttpStatus.UNAUTHORIZED);
        
        // WebSocket specific exceptions
        //STATUS_MAP.put(org.springframework.web.socket.WebSocketException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(MessageDeliveryException.class, HttpStatus.SERVICE_UNAVAILABLE);
        STATUS_MAP.put(org.springframework.messaging.MessagingException.class, HttpStatus.BAD_REQUEST);
        
        // Kafka specific exceptions
        STATUS_MAP.put(KafkaException.class, HttpStatus.SERVICE_UNAVAILABLE);
        STATUS_MAP.put(org.apache.kafka.common.KafkaException.class, HttpStatus.SERVICE_UNAVAILABLE);
        STATUS_MAP.put(org.springframework.kafka.KafkaException.class, HttpStatus.SERVICE_UNAVAILABLE);
        STATUS_MAP.put(org.apache.kafka.common.errors.SerializationException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(org.apache.kafka.common.errors.TimeoutException.class, HttpStatus.REQUEST_TIMEOUT);
        
        // JSON processing exceptions
        STATUS_MAP.put(com.fasterxml.jackson.core.JsonProcessingException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(com.fasterxml.jackson.databind.JsonMappingException.class, HttpStatus.BAD_REQUEST);
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> ea = super.getErrorAttributes(webRequest, options);

        Throwable error = getError(webRequest);
        ea.put("status", getStatus(error));
        
        if (error != null) {
            // Set error code based on exception type
            switch (error) {
                case BaseException ex -> ea.put(ATTR_MESSAGE_CODE, ex.getCode());
                case ConstraintViolationException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_INVALID_INPUT);
                case MethodArgumentNotValidException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_INVALID_INPUT);
                case BindException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_INVALID_INPUT);
                case JwtValidationException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_INVALID_JWT_TOKEN);
                case BadCredentialsException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_ACCESS_DENIED);
                case KafkaException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_KAFKA_CONSUMER_ERROR);
                case MessageDeliveryException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_MESSAGE_BROADCAST_FAILED);
                default -> ea.put(ATTR_MESSAGE_CODE, error.getMessage());
            }

            // Set additional error data based on exception type
            switch (error) {
                case BaseException ex -> {
                    ea.put("data", ex.getData());
                    addBroadcastingServiceContext(ea, ex);
                }
                case ConstraintViolationException ex -> ea.put(ATTR_VALIDATIONS, ex.getConstraintViolations()
                    .stream().map(ViolatedConstraint::new).collect(Collectors.toSet()));
                case MethodArgumentNotValidException ex -> ea.put(ATTR_VALIDATIONS, ex.getBindingResult().getFieldErrors()
                    .stream().map(ViolatedConstraint::new).collect(Collectors.toSet()));
                case BindException ex -> ea.put(ATTR_VALIDATIONS, ex.getBindingResult().getFieldErrors()
                    .stream().map(ViolatedConstraint::new).collect(Collectors.toSet()));
                case JwtValidationException ex -> ea.put("data", Map.of("jwtError", ex.getMessage()));
                case KafkaException ex -> ea.put("data", Map.of("kafkaError", ex.getMessage()));
                case MessageDeliveryException ex -> ea.put("data", Map.of("messageError", ex.getMessage()));
                default -> {}
            }
        }
        
        // Add service-specific metadata
        ea.put("service", "finstream-broadcasting-service");
        ea.put("version", "1.0.0");
        
        return ea;
    }

    /**
     * Adds Broadcasting Service specific context to error attributes.
     */
    private void addBroadcastingServiceContext(Map<String, Object> errorAttributes, BaseException ex) {
        Map<String, Object> serviceContext = new HashMap<>();
        
        // Add WebSocket context if present
        if (ex.getData().containsKey("connectionId")) {
            serviceContext.put("websocketConnection", ex.getData().get("connectionId"));
        }
        
        // Add Kafka context if present
        if (ex.getData().containsKey("topic")) {
            serviceContext.put("kafkaTopic", ex.getData().get("topic"));
        }
        if (ex.getData().containsKey("consumerGroup")) {
            serviceContext.put("kafkaConsumerGroup", ex.getData().get("consumerGroup"));
        }
        
        // Add stock symbol context if present
        if (ex.getData().containsKey("symbol")) {
            serviceContext.put("stockSymbol", ex.getData().get("symbol"));
        }
        
        // Add user context if present
        if (ex.getData().containsKey("userId")) {
            serviceContext.put("userId", ex.getData().get("userId"));
        }
        
        if (!serviceContext.isEmpty()) {
            errorAttributes.put("serviceContext", serviceContext);
        }
    }

    /**
     * Extracts the HTTP status code from the thrown exception.
     *
     * @return HTTP status corresponding to the thrown exception, or 500 (INTERNAL_SERVER_ERROR) otherwise.
     */
    public HttpStatus getStatus(Throwable error) {
        if (error instanceof BaseException exception) {
            return exception.getStatus();
        }
        else if (error != null && STATUS_MAP.containsKey(error.getClass())) {
            return STATUS_MAP.get(error.getClass());
        }
        else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Record representing a violated constraint for validation errors.
     */
    public static record ViolatedConstraint(String code, String path, Object value) {
        public ViolatedConstraint(ConstraintViolation<?> violation) {
            this(violation.getMessage(), violation.getPropertyPath().toString(), violation.getInvalidValue());
        }

        public ViolatedConstraint(FieldError violation) {
            this(violation.getDefaultMessage(), violation.getField(), violation.getRejectedValue());
        }
    }
}
