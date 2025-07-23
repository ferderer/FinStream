package pro.finstream.broadcasting.domain.stomp;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pro.finstream.broadcasting.domain.stockprice.StockPrice;

/**
 * Stock Broadcasting Service for Real-time STOMP WebSocket Updates
 * 
 * Integrates with Kafka StockPriceConsumerService to broadcast stock price updates
 * to connected Angular frontend clients via STOMP WebSocket protocol.
 * 
 * Banking Features:
 * - Real-time stock price broadcasting to all subscribers
 * - Connection statistics and performance monitoring
 * - User session tracking for audit compliance
 * - Future: User-specific watchlist filtering
 * 
 * Message Flow:
 * Kafka Consumer → StockBroadcastingService → STOMP WebSocket → Angular Frontend
 * 
 * STOMP Destinations:
 * - /topic/stocks/prices - Public broadcast for all stock prices
 * - /user/topic/watchlist - User-specific updates (future integration)
 * 
 * @author FinStream Platform
 */
@Service
public class StockBroadcastingService {

    private static final Logger log = LoggerFactory.getLogger(StockBroadcastingService.class);
    
    // STOMP WebSocket destinations
    private static final String TOPIC_STOCK_PRICES = "/topic/stocks/prices";
    private static final String TOPIC_USER_WATCHLIST = "/topic/watchlist";
    
    private final SimpMessagingTemplate messagingTemplate;
    
    // Performance and monitoring metrics
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalStockUpdates = new AtomicLong(0);
    private final Map<String, AtomicLong> symbolUpdateCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastUpdateTimes = new ConcurrentHashMap<>();
    
    public StockBroadcastingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        log.info("StockBroadcastingService initialized - ready for real-time stock price broadcasting");
    }

    /**
     * Broadcast stock price update to all connected WebSocket clients
     * 
     * Called by StockPriceConsumerService when new stock price received from Kafka.
     * Sends STOMP MESSAGE frame to all clients subscribed to /topic/stocks/prices
     * 
     * @param stockPrice Real-time stock price from Kafka consumer
     */
    public void broadcastStockPrice(StockPrice stockPrice) {
        try {
            if (stockPrice == null) {
                log.warn("Attempted to broadcast null stock price - skipping");
                return;
            }
            
            // Validate required fields for banking compliance
            if (stockPrice.symbol() == null || stockPrice.symbol().isBlank()) {
                log.error("Stock price missing symbol - broadcast rejected: {}", stockPrice);
                return;
            }
            
            // Broadcast to all subscribers
            messagingTemplate.convertAndSend(TOPIC_STOCK_PRICES, stockPrice);
            
            // Update performance metrics
            updateMetrics(stockPrice);
            
            log.debug("Stock price broadcasted: symbol={}, price={}, subscribers={}", 
                    stockPrice.symbol(), stockPrice.price(), getActiveSubscriberCount());
                    
        }
        catch (MessagingException e) {
            log.error("Failed to broadcast stock price for symbol {}: {}", 
                    stockPrice != null ? stockPrice.symbol() : "unknown", e.getMessage(), e);
        }
    }
    
    /*
     * Broadcast user-specific watchlist update
     * 
     * Future integration with Watchlist Service (Jakarta EE Payara).
     * Will consume watchlist events from Kafka and send targeted updates.
     * 
     * @param userId User identifier from JWT token
     * @param event Watchlist event (ADD, REMOVE, UPDATE)
     */
    public void broadcastWatchlistUpdate(String userId, Object event) {
        try {
            if (userId == null || userId.isBlank()) {
                log.warn("Attempted to broadcast watchlist update without userId - skipping");
                return;
            }
            
            // Send to specific user's subscription
            messagingTemplate.convertAndSendToUser(userId, TOPIC_USER_WATCHLIST, event);
            
            totalMessagesSent.incrementAndGet();
            log.debug("Watchlist update broadcasted to user: userId={}, event={}", userId, event);
            
        }
        catch (MessagingException e) {
            log.error("Failed to broadcast watchlist update to user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Send system notification to all connected clients
     * 
     * Banking use cases:
     * - Market status changes (OPEN, CLOSED, HALTED)
     * - System maintenance notifications
     * - Trading alerts and compliance messages
     * 
     * @param notification System notification object
     */
    public void broadcastSystemNotification(SystemNotification notification) {
        try {
            messagingTemplate.convertAndSend("/topic/system/notifications", notification);
            totalMessagesSent.incrementAndGet();
            
            log.info("System notification broadcasted: type={}, message={}", 
                    notification.type(), notification.message());
                    
        }
        catch (MessagingException e) {
            log.error("Failed to broadcast system notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update performance and monitoring metrics
     * 
     * Banking compliance requires detailed audit trails and performance monitoring.
     * Tracks message frequency, symbol distribution, and system health.
     */
    private void updateMetrics(StockPrice stockPrice) {
        totalStockUpdates.incrementAndGet();
        totalMessagesSent.incrementAndGet();
        
        String symbol = stockPrice.symbol();
        symbolUpdateCounts.computeIfAbsent(symbol, k -> new AtomicLong(0)).incrementAndGet();
        lastUpdateTimes.put(symbol, Instant.now());
        
        // Log performance metrics every 100 messages
        long totalUpdates = totalStockUpdates.get();
        if (totalUpdates % 100 == 0) {
            log.info("Broadcasting performance: totalUpdates={}, uniqueSymbols={}, avgUpdatesPerSymbol={}", 
                    totalUpdates, symbolUpdateCounts.size(), 
                    symbolUpdateCounts.size() > 0 ? totalUpdates / symbolUpdateCounts.size() : 0);
        }
    }
    
    /**
     * Get current active subscriber count
     * 
     * Note: SimpMessagingTemplate doesn't provide direct subscriber count.
     * In production, integrate with Spring Session or Redis for accurate metrics.
     * 
     * @return Estimated active subscriber count
     */
    private int getActiveSubscriberCount() {
        // TODO: Integrate with session management for accurate count
        // For now, return placeholder for logging purposes
        return -1; // Unknown - requires session integration
    }
    
    /**
     * Get broadcasting performance statistics
     * 
     * Banking operations require detailed performance monitoring and SLA tracking.
     * Used by health checks and monitoring dashboards.
     * 
     * @return Performance statistics map
     */
    public Map<String, Object> getPerformanceStats() {
        return Map.of(
            "totalMessagesSent", totalMessagesSent.get(),
            "totalStockUpdates", totalStockUpdates.get(),
            "uniqueSymbolsTracked", symbolUpdateCounts.size(),
            "symbolUpdateCounts", Map.copyOf(symbolUpdateCounts),
            "lastUpdateTimes", Map.copyOf(lastUpdateTimes),
            "serviceUptime", Instant.now().toString()
        );
    }
    
    /**
     * System notification record for broadcasting
     * 
     * Used for market status updates, maintenance windows, and compliance alerts.
     */
    public record SystemNotification(
        String type,
        String message,
        String severity,
        Instant timestamp
    ) {
        public SystemNotification(String type, String message, String severity) {
            this(type, message, severity, Instant.now());
        }
    }
}
