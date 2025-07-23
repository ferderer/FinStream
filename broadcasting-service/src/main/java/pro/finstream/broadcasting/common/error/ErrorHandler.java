package pro.finstream.broadcasting.common.error;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/**
 * Global error handler for the FinStream Broadcasting Service.
 */
@RestController
@RequiredArgsConstructor
public class ErrorHandler {

    private final CustomErrorAttributes ea;
    private static final ErrorAttributeOptions ALL_ERROR_ATTRIBUTES = ErrorAttributeOptions.of();

    @GetMapping(path = {"/error", "/error/**"})
    public Map<String, Object> genericJsonError(WebRequest request) {
        return ea.getErrorAttributes(request, ALL_ERROR_ATTRIBUTES);
    }

    /**
     * Specific endpoint for 404 errors (static files, etc.).
     */
    @GetMapping(path = "/error/404")
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public String staticFileNotFound() {
        return "No such file";
    }

    /**
     * WebSocket specific error endpoint.
     */
    @GetMapping(path = "/error/websocket")
    public Map<String, Object> webSocketError(WebRequest request) {
        Map<String, Object> errorAttributes = ea.getErrorAttributes(request, ALL_ERROR_ATTRIBUTES);
        
        // Add WebSocket-specific error context
        errorAttributes.put("errorType", "WEBSOCKET_ERROR");
        errorAttributes.put("reconnectionAdvice", "Client should attempt reconnection with exponential backoff");
        
        return errorAttributes;
    }

    /**
     * Kafka specific error endpoint.
     */
    @GetMapping(path = "/error/kafka")
    public Map<String, Object> kafkaError(WebRequest request) {
        Map<String, Object> errorAttributes = ea.getErrorAttributes(request, ALL_ERROR_ATTRIBUTES);
        
        // Add Kafka-specific error context
        errorAttributes.put("errorType", "KAFKA_ERROR");
        errorAttributes.put("serviceImpact", "Real-time stock updates may be delayed");
        
        return errorAttributes;
    }

    /**
     * Authentication/Authorization error endpoint.
     */
    @GetMapping(path = "/error/auth")
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public Map<String, Object> authError(WebRequest request) {
        Map<String, Object> errorAttributes = ea.getErrorAttributes(request, ALL_ERROR_ATTRIBUTES);
        
        // Add authentication-specific error context
        errorAttributes.put("errorType", "AUTHENTICATION_ERROR");
        errorAttributes.put("authAdvice", "Please obtain a valid JWT token from the SSO service");
        
        return errorAttributes;
    }
}
