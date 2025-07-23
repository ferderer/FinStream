/**
 * TypeScript Interfaces for FinStream Stock Price System
 * 
 * Provides type safety for real-time stock data flowing from:
 * Kafka → Broadcasting Service → WebSocket → Angular Frontend
 * 
 * Banking Compliance:
 * - Strict typing prevents data corruption
 * - Required fields ensure data integrity
 * - Timestamp tracking for audit trails
 * - Consistent data structures across services
 * 
 * @author FinStream Platform
 */

/**
 * Real-time stock price data from Finnhub via WebSocket
 * 
 * Matches StockPrice record from Broadcasting Service Java backend.
 * Used for live price updates in dashboard components.
 */
export interface StockPrice {
  /** Stock symbol (e.g., 'AAPL', 'GOOGL') */
  symbol: string;
  
  /** Current stock price in USD */
  price: number;
  
  /** Price change from previous close in USD */
  change: number;
  
  /** Price change percentage from previous close */
  changePercent: number;
  
  /** Day's highest price */
  high: number;
  
  /** Day's lowest price */
  low: number;
  
  /** Original timestamp from Finnhub API */
  timestamp: string;
  
  /** Data source identifier (e.g., 'finnhub') */
  source: string;
  
  /** Processing timestamp from Broadcasting Service */
  processedAt: string;
}

/**
 * Stock price with UI-specific metadata
 * 
 * Extends base StockPrice with frontend display logic and animations.
 * Used by StockCard and StockGrid components for enhanced user experience.
 */
export interface EnhancedStockPrice extends StockPrice {
  /** Previous price for change calculation and animations */
  previousPrice?: number;
  
  /** Price trend direction for visual indicators */
  trend: PriceTrend;
  
  /** Last update timestamp for UI freshness indicators */
  lastUpdated: Date;
  
  /** Animation state for smooth UI transitions */
  animationState: AnimationState;
  
  /** Color coding based on price movement */
  displayColor: PriceColor;
  
  /** Formatted price strings for display */
  formatted: FormattedPriceData;
}

/**
 * Price movement trend indicators
 */
export type PriceTrend = 'up' | 'down' | 'neutral' | 'unknown';

/**
 * Animation states for smooth UI updates
 */
export type AnimationState = 'idle' | 'updating' | 'flash-green' | 'flash-red';

/**
 * Banking-standard color coding for price changes
 */
export type PriceColor = 'gain' | 'loss' | 'neutral' | 'warning';

/**
 * Pre-formatted price data for consistent display
 * 
 * Banking applications require consistent number formatting
 * for regulatory compliance and professional presentation.
 */
export interface FormattedPriceData {
  /** Price formatted as currency (e.g., '$150.25') */
  price: string;
  
  /** Change formatted with sign (e.g., '+$2.15', '-$1.20') */
  change: string;
  
  /** Percentage change formatted (e.g., '+1.45%', '-0.82%') */
  changePercent: string;
  
  /** High price formatted as currency */
  high: string;
  
  /** Low price formatted as currency */
  low: string;
  
  /** Relative time since last update (e.g., '2 seconds ago') */
  lastUpdated: string;
}

/**
 * WebSocket connection status for monitoring
 * 
 * Critical for banking applications that require real-time data reliability.
 * Used by ConnectionStatusComponent and monitoring dashboards.
 */
export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error' | 'reconnecting';

/**
 * WebSocket connection statistics and health metrics
 */
export interface ConnectionStats {
  /** Current connection status */
  status: ConnectionStatus;
  
  /** Number of reconnection attempts */
  reconnectAttempts: number;
  
  /** Count of active stock symbols receiving updates */
  connectedSymbols: number;
  
  /** Total price updates received */
  totalUpdates: number;
  
  /** Connection duration in milliseconds */
  connectionDuration: number;
  
  /** Last error message if any */
  lastError?: string;
  
  /** Connection established timestamp */
  connectedAt?: Date;
  
  /** Average updates per second */
  updatesPerSecond: number;
}

/**
 * System notification from Broadcasting Service
 * 
 * Used for market status updates, maintenance windows, and compliance alerts.
 */
export interface SystemNotification {
  /** Notification type (e.g., 'MARKET_STATUS', 'SYSTEM_MAINTENANCE') */
  type: string;
  
  /** Human-readable notification message */
  message: string;
  
  /** Severity level for UI styling */
  severity: NotificationSeverity;
  
  /** Notification timestamp */
  timestamp: string;
  
  /** Optional action button configuration */
  action?: NotificationAction;
}

/**
 * Notification severity levels for UI styling
 */
export type NotificationSeverity = 'info' | 'warning' | 'error' | 'success';

/**
 * Optional notification action configuration
 */
export interface NotificationAction {
  /** Action button label */
  label: string;
  
  /** Action handler function name */
  handler: string;
  
  /** Additional action parameters */
  params?: Record<string, any>;
}

/**
 * Stock watchlist item for future Watchlist Service integration
 * 
 * Prepared for Jakarta EE Watchlist Service integration.
 * Will be used when users can add/remove stocks from personal watchlists.
 */
export interface WatchlistItem {
  /** User identifier from JWT token */
  userId: string;
  
  /** Stock symbol */
  symbol: string;
  
  /** When item was added to watchlist */
  addedAt: Date;
  
  /** Optional price alert threshold */
  alertThreshold?: number;
  
  /** Alert type (above, below, change_percent) */
  alertType?: 'above' | 'below' | 'change_percent';
  
  /** Whether alerts are enabled for this item */
  alertsEnabled: boolean;
}

/**
 * Watchlist event from Kafka (future integration)
 * 
 * Events published by Jakarta EE Watchlist Service and consumed
 * by Broadcasting Service for user-specific WebSocket updates.
 */
export interface WatchlistEvent {
  /** Event type */
  action: WatchlistAction;
  
  /** User identifier */
  userId: string;
  
  /** Stock symbol affected */
  symbol: string;
  
  /** Event timestamp */
  timestamp: string;
  
  /** Optional additional event data */
  metadata?: Record<string, any>;
}

/**
 * Watchlist action types
 */
export type WatchlistAction = 'ADD' | 'REMOVE' | 'UPDATE' | 'ALERT_TRIGGERED';

/**
 * Error types for WebSocket and stock price operations
 */
export interface StockServiceError {
  /** Error code for programmatic handling */
  code: StockErrorCode;
  
  /** Human-readable error message */
  message: string;
  
  /** Original error object if available */
  originalError?: any;
  
  /** Error timestamp */
  timestamp: Date;
  
  /** Context information for debugging */
  context?: Record<string, any>;
}

/**
 * Stock service error codes
 */
export type StockErrorCode = 
  | 'WEBSOCKET_CONNECTION_FAILED'
  | 'WEBSOCKET_AUTHENTICATION_FAILED' 
  | 'WEBSOCKET_MESSAGE_PARSE_ERROR'
  | 'STOCK_PRICE_VALIDATION_ERROR'
  | 'NETWORK_ERROR'
  | 'JWT_TOKEN_EXPIRED'
  | 'UNKNOWN_ERROR';

/**
 * Market status information
 * 
 * Used to indicate whether markets are open, closed, or in pre/after hours trading.
 */
export interface MarketStatus {
  /** Market identifier (e.g., 'NYSE', 'NASDAQ') */
  market: string;
  
  /** Current market status */
  status: MarketState;
  
  /** Next market open time */
  nextOpen?: Date;
  
  /** Next market close time */
  nextClose?: Date;
  
  /** Current market timezone */
  timezone: string;
}

/**
 * Market status states
 */
export type MarketState = 'OPEN' | 'CLOSED' | 'PRE_MARKET' | 'AFTER_HOURS' | 'HOLIDAY';
