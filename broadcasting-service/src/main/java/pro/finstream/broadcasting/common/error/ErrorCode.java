package pro.finstream.broadcasting.common.error;

/**
 * Standardized error codes for financial data processing in the FinStream Broadcasting Service.
 */
public enum ErrorCode {
    
    // Authentication & Authorization
    E_ACCESS_DENIED,
    E_INVALID_JWT_TOKEN,
    E_JWT_TOKEN_EXPIRED,
    E_JWT_TOKEN_MALFORMED,
    E_UNAUTHORIZED_WEBSOCKET_CONNECTION,
    E_USER_SESSION_NOT_FOUND,
    E_SESSION_LIMIT_EXCEEDED,
    
    // WebSocket Communication
    E_WEBSOCKET_CONNECTION_FAILED,
    E_WEBSOCKET_CONNECTION_LOST,
    E_WEBSOCKET_MESSAGE_INVALID,
    E_WEBSOCKET_SUBSCRIPTION_FAILED,
    E_MESSAGE_BROADCAST_FAILED,
    E_CLIENT_CONNECTION_TIMEOUT,
    
    // Stock Data Processing
    E_STOCK_PRICE_INVALID_FORMAT,
    E_STOCK_PRICE_MISSING_SYMBOL,
    E_STOCK_PRICE_PROCESSING_ERROR,
    E_STOCK_DATA_SERIALIZATION_ERROR,
    E_STOCK_SYMBOL_NOT_SUPPORTED,
    
    // Kafka Integration
    E_KAFKA_CONSUMER_ERROR,
    E_KAFKA_CONNECTION_FAILED,
    E_KAFKA_MESSAGE_DESERIALIZATION_ERROR,
    E_KAFKA_TOPIC_NOT_FOUND,
    E_KAFKA_CONSUMER_GROUP_ERROR,
    E_KAFKA_OFFSET_COMMIT_FAILED,
    
    // System & Infrastructure
    E_INTERNAL_ERROR,
    E_SERVICE_UNAVAILABLE,
    E_RESOURCE_LIMIT_EXCEEDED,
    E_CONFIGURATION_ERROR,
    E_HEALTH_CHECK_FAILED,
    
    // Input Validation
    E_INVALID_INPUT,
    E_PARAM_REQUIRED,
    E_INVALID_SYMBOL_FORMAT,
    E_INVALID_MESSAGE_TYPE,
    E_MESSAGE_TOO_LARGE,
    
    // Rate Limiting & Throttling
    E_RATE_LIMIT_EXCEEDED,
    E_CONNECTION_LIMIT_EXCEEDED,
    E_MESSAGE_RATE_EXCEEDED,
    E_BANDWIDTH_LIMIT_EXCEEDED,
    
    // Not Implemented/Supported
    E_NOT_IMPLEMENTED,
    E_NOT_SUPPORTED,
    E_FEATURE_DISABLED
}
