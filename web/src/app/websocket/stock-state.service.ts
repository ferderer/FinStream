import { Injectable, computed, signal, effect } from '@angular/core';
import { 
  StockPrice, 
  EnhancedStockPrice, 
  PriceTrend, 
  AnimationState, 
  PriceColor, 
  FormattedPriceData,
  ConnectionStats,
  SystemNotification,
  MarketStatus,
  StockServiceError
} from './stock-price.types';

/**
 * Stock State Service - Centralized Reactive State Management
 * 
 * Manages all stock price data and UI state using Angular Signals.
 * Integrates with WebSocketService to provide enhanced stock data
 * with animations, formatting, and banking-specific features.
 * 
 * Banking Features:
 * - Real-time price tracking with audit trail
 * - Professional number formatting for compliance
 * - Color-coded price movements for quick analysis
 * - Performance monitoring and error handling
 * - Market status integration for trading hours
 * 
 * Architecture:
 * WebSocket → StockStateService → UI Components
 * 
 * Usage:
 * ```typescript
 * constructor(private stockState: StockStateService) {
 *   effect(() => {
 *     const stocks = this.stockState.enhancedStocks();
 *     this.updateUI(stocks);
 *   });
 * }
 * ```
 * 
 * @author FinStream Platform
 */
@Injectable({
  providedIn: 'root'
})
export class StockStateService {
  
  // Core reactive state using Angular Signals
  private readonly _rawStockPrices = signal<Map<string, StockPrice>>(new Map());
  private readonly _enhancedStocks = signal<Map<string, EnhancedStockPrice>>(new Map());
  private readonly _selectedSymbols = signal<Set<string>>(new Set(['AAPL', 'GOOGL', 'MSFT', 'TSLA', 'NVDA']));
  private readonly _systemNotifications = signal<SystemNotification[]>([]);
  private readonly _marketStatus = signal<MarketStatus | null>(null);
  private readonly _errors = signal<StockServiceError[]>([]);
  
  // Performance and monitoring
  private readonly _totalUpdates = signal<number>(0);
  private readonly _lastUpdateTime = signal<Date | null>(null);
  private readonly _updateFrequency = signal<number>(0); // updates per second
  
  // Public readonly signals for components
  public readonly rawStockPrices = this._rawStockPrices.asReadonly();
  public readonly enhancedStocks = this._enhancedStocks.asReadonly();
  public readonly selectedSymbols = this._selectedSymbols.asReadonly();
  public readonly systemNotifications = this._systemNotifications.asReadonly();
  public readonly marketStatus = this._marketStatus.asReadonly();
  public readonly errors = this._errors.asReadonly();
  
  // Computed signals for derived state
  public readonly enhancedStocksArray = computed(() => 
    Array.from(this.enhancedStocks().values())
  );
  
  public readonly selectedStocks = computed(() => 
    this.enhancedStocksArray().filter(stock => 
      this.selectedSymbols().has(stock.symbol)
    )
  );
  
  public readonly gainers = computed(() => 
    this.enhancedStocksArray()
      .filter(stock => stock.change > 0)
      .sort((a, b) => b.changePercent - a.changePercent)
  );
  
  public readonly losers = computed(() => 
    this.enhancedStocksArray()
      .filter(stock => stock.change < 0)  
      .sort((a, b) => a.changePercent - b.changePercent)
  );
  
  public readonly totalStocksTracked = computed(() => this.enhancedStocks().size);
  
  public readonly activeNotifications = computed(() => 
    this.systemNotifications().filter(n => 
      Date.now() - new Date(n.timestamp).getTime() < 300_000 // 5 minutes
    )
  );
  
  public readonly latestErrors = computed(() => 
    this.errors().slice(-5) // Last 5 errors
  );
  
  public readonly performanceStats = computed((): ConnectionStats => ({
    status: 'connected', // Will be updated by WebSocket service
    reconnectAttempts: 0,
    connectedSymbols: this.totalStocksTracked(),
    totalUpdates: this._totalUpdates(),
    connectionDuration: this._lastUpdateTime() ? 
      Date.now() - this._lastUpdateTime()!.getTime() : 0,
    updatesPerSecond: this._updateFrequency(),
    connectedAt: this._lastUpdateTime() || undefined
  }));
  
  // Update frequency calculation
  private updateTimeHistory: number[] = [];
  
  constructor() {
    console.log('StockStateService initialized with reactive state management');
    
    // Setup performance monitoring
    this.setupPerformanceMonitoring();
    
    // Initialize with default selected symbols
    this.initializeDefaultSymbols();
  }
  
  /**
   * Update stock price from WebSocket service
   * 
   * Main integration point with WebSocketService.
   * Processes raw stock price and creates enhanced version with UI metadata.
   * 
   * @param stockPrice Raw stock price from WebSocket
   */
  public updateStockPrice(stockPrice: StockPrice): void {
    try {
      // Validate input
      if (!this.validateStockPrice(stockPrice)) {
        this.addError({
          code: 'STOCK_PRICE_VALIDATION_ERROR',
          message: `Invalid stock price data for symbol: ${stockPrice?.symbol}`,
          timestamp: new Date(),
          context: { stockPrice }
        });
        return;
      }
      
      // Update raw prices map
      const currentRawPrices = new Map(this._rawStockPrices());
      const previousPrice = currentRawPrices.get(stockPrice.symbol);
      currentRawPrices.set(stockPrice.symbol, stockPrice);
      this._rawStockPrices.set(currentRawPrices);
      
      // Create enhanced stock price with UI metadata
      const enhancedStock = this.createEnhancedStockPrice(stockPrice, previousPrice);
      
      // Update enhanced stocks map
      const currentEnhancedStocks = new Map(this._enhancedStocks());
      currentEnhancedStocks.set(stockPrice.symbol, enhancedStock);
      this._enhancedStocks.set(currentEnhancedStocks);
      
      // Update performance metrics
      this.updatePerformanceMetrics();
      
      console.debug(`Stock updated: ${stockPrice.symbol} = $${stockPrice.price} (${stockPrice.changePercent}%)`);
      
    } catch (error) {
      this.addError({
        code: 'UNKNOWN_ERROR',
        message: 'Failed to process stock price update',
        originalError: error,
        timestamp: new Date(),
        context: { stockPrice }
      });
    }
  }
  
  /**
   * Add system notification
   * 
   * Used by WebSocket service for market updates and system messages.
   */
  public addSystemNotification(notification: SystemNotification): void {
    const current = [...this.systemNotifications()];
    current.unshift(notification); // Add to beginning
    
    // Keep only last 50 notifications
    if (current.length > 50) {
      current.splice(50);
    }
    
    this._systemNotifications.set(current);
    console.info('System notification added:', notification.message);
  }
  
  /**
   * Update market status
   * 
   * Used to indicate trading hours and market conditions.
   */
  public updateMarketStatus(status: MarketStatus): void {
    this._marketStatus.set(status);
    console.info(`Market status updated: ${status.market} - ${status.status}`);
  }
  
  /**
   * Add or remove symbol from selected tracking list
   */
  public toggleSymbol(symbol: string): void {
    const current = new Set(this.selectedSymbols());
    
    if (current.has(symbol)) {
      current.delete(symbol);
    } else {
      current.add(symbol);
    }
    
    this._selectedSymbols.set(current);
    console.log(`Symbol ${symbol} ${current.has(symbol) ? 'added to' : 'removed from'} tracking`);
  }
  
  /**
   * Get enhanced stock price for specific symbol
   */
  public getEnhancedStock(symbol: string): EnhancedStockPrice | undefined {
    return this.enhancedStocks().get(symbol);
  }
  
  /**
   * Clear all errors
   */
  public clearErrors(): void {
    this._errors.set([]);
  }
  
  /**
   * Clear system notifications
   */
  public clearNotifications(): void {
    this._systemNotifications.set([]);
  }
  
  /**
   * Create enhanced stock price with UI metadata
   */
  private createEnhancedStockPrice(
    stockPrice: StockPrice, 
    previous?: StockPrice
  ): EnhancedStockPrice {
    
    const trend = this.calculateTrend(stockPrice, previous);
    const animationState = this.determineAnimationState(stockPrice, previous);
    const displayColor = this.determineDisplayColor(stockPrice);
    const formatted = this.formatPriceData(stockPrice);
    
    return {
      ...stockPrice,
      previousPrice: previous?.price,
      trend,
      lastUpdated: new Date(),
      animationState,
      displayColor,
      formatted
    };
  }
  
  /**
   * Calculate price trend direction
   */
  private calculateTrend(current: StockPrice, previous?: StockPrice): PriceTrend {
    if (!previous) return 'unknown';
    
    if (current.price > previous.price) return 'up';
    if (current.price < previous.price) return 'down';
    return 'neutral';
  }
  
  /**
   * Determine animation state for UI effects
   */
  private determineAnimationState(current: StockPrice, previous?: StockPrice): AnimationState {
    if (!previous) return 'idle';
    
    if (current.price > previous.price) return 'flash-green';
    if (current.price < previous.price) return 'flash-red';
    return 'updating';
  }
  
  /**
   * Determine color coding for price display
   */
  private determineDisplayColor(stockPrice: StockPrice): PriceColor {
    if (stockPrice.change > 0) return 'gain';
    if (stockPrice.change < 0) return 'loss';
    return 'neutral';
  }
  
  /**
   * Format price data for consistent banking display
   */
  private formatPriceData(stockPrice: StockPrice): FormattedPriceData {
    const currencyFormatter = new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    const percentFormatter = new Intl.NumberFormat('en-US', {
      style: 'percent',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
      signDisplay: 'always'
    });
    
    const changeSign = stockPrice.change >= 0 ? '+' : '';
    
    return {
      price: currencyFormatter.format(stockPrice.price),
      change: `${changeSign}${currencyFormatter.format(stockPrice.change)}`,
      changePercent: percentFormatter.format(stockPrice.changePercent / 100),
      high: currencyFormatter.format(stockPrice.high),
      low: currencyFormatter.format(stockPrice.low),
      lastUpdated: 'Just now' // Will be updated by time pipe
    };
  }
  
  /**
   * Validate stock price data integrity
   */
  private validateStockPrice(stockPrice: StockPrice): boolean {
    if (!stockPrice || typeof stockPrice !== 'object') return false;
    if (!stockPrice.symbol || typeof stockPrice.symbol !== 'string') return false;
    if (stockPrice.price == null || typeof stockPrice.price !== 'number') return false;
    if (stockPrice.change == null || typeof stockPrice.change !== 'number') return false;
    if (stockPrice.changePercent == null || typeof stockPrice.changePercent !== 'number') return false;
    if (isNaN(stockPrice.price) || !isFinite(stockPrice.price)) return false;
    
    return true;
  }
  
  /**
   * Add error to error tracking
   * 
   * Public method for external services (like WebSocketService) to report errors.
   */
  public addError(error: StockServiceError): void {
    const current = [...this.errors()];
    current.unshift(error);
    
    // Keep only last 20 errors
    if (current.length > 20) {
      current.splice(20);
    }
    
    this._errors.set(current);
    console.error('StockState Error:', error.message, error);
  }
  
  /**
   * Update performance metrics for monitoring
   */
  private updatePerformanceMetrics(): void {
    const now = Date.now();
    this._totalUpdates.update(count => count + 1);
    this._lastUpdateTime.set(new Date());
    
    // Calculate updates per second
    this.updateTimeHistory.push(now);
    
    // Keep only last 10 seconds of history
    const tenSecondsAgo = now - 10_000;
    this.updateTimeHistory = this.updateTimeHistory.filter(time => time > tenSecondsAgo);
    
    const updatesPerSecond = this.updateTimeHistory.length / 10;
    this._updateFrequency.set(updatesPerSecond);
  }
  
  /**
   * Setup performance monitoring effects
   */
  private setupPerformanceMonitoring(): void {
    // Log performance stats every minute
    setInterval(() => {
      const stats = this.performanceStats();
      console.info('Stock State Performance:', {
        totalStocks: stats.connectedSymbols,
        totalUpdates: stats.totalUpdates,
        updatesPerSecond: stats.updatesPerSecond.toFixed(2),
        errors: this.errors().length
      });
    }, 60_000); // 1 minute
  }
  
  /**
   * Initialize with default selected symbols for demo
   */
  private initializeDefaultSymbols(): void {
    const defaultSymbols = new Set(['AAPL', 'GOOGL', 'MSFT', 'TSLA', 'NVDA']);
    this._selectedSymbols.set(defaultSymbols);
    
    console.log('Default symbols initialized:', Array.from(defaultSymbols));
  }
}
