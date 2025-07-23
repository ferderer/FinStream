import { Injectable, computed, signal, inject } from '@angular/core';
import { StockStateService } from './stock-state.service';
import { StockPrice, SystemNotification, ConnectionStatus } from './stock-price.types';
import SockJS from 'sockjs-client';
import { Client, Frame, Message } from '@stomp/stompjs';
import { OAuthService } from 'angular-oauth2-oidc';

/**
 * WebSocket Service for Real-time Stock Price Updates
 * 
 * Integrates with FinStream Broadcasting Service via STOMP WebSocket protocol.
 * Provides reactive state management using Angular signals for real-time UI updates.
 * 
 * Features:
 * - JWT authentication integration with existing OAuth2 flow
 * - Automatic reconnection for banking-grade reliability  
 * - Angular Signals for reactive stock price state
 * - Error handling and connection status monitoring
 * - Performance optimized for high-frequency updates
 * 
 * Usage:
 * ```typescript
 * constructor(private wsService: WebSocketService) {
 *   effect(() => {
 *     const prices = this.wsService.stockPrices();
 *     console.log('New prices:', prices);
 *   });
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private readonly authService = inject(OAuthService);
  private readonly stockStateService = inject(StockStateService);
  
  // STOMP client for WebSocket communication
  private stompClient: Client | null = null;
  
  // Connection configuration
  private readonly WS_URL = 'http://localhost:8082/stock-updates';
  private readonly RECONNECT_DELAY = 5000; // 5 seconds
  private readonly MAX_RECONNECT_ATTEMPTS = 10;
  
  // Reactive connection state using Angular Signals
  private readonly _connectionStatus = signal<ConnectionStatus>('disconnected');
  private readonly _error = signal<string | null>(null);
  private readonly _reconnectAttempts = signal<number>(0);
  
  // Public readonly signals for components
  public readonly connectionStatus = this._connectionStatus.asReadonly();
  public readonly error = this._error.asReadonly();
  public readonly reconnectAttempts = this._reconnectAttempts.asReadonly();
  
  // Computed signals for derived connection state
  public readonly isConnected = computed(() => this.connectionStatus() === 'connected');
  public readonly isConnecting = computed(() => this.connectionStatus() === 'connecting');
  public readonly canReconnect = computed(() => 
    this.reconnectAttempts() < this.MAX_RECONNECT_ATTEMPTS && 
    !this.isConnected() && 
    !this.isConnecting()
  );
  
  // Delegate stock data access to StockStateService
  public readonly stockPrices = this.stockStateService.enhancedStocks;
  public readonly selectedStocks = this.stockStateService.selectedStocks;
  public readonly performanceStats = this.stockStateService.performanceStats;
  
  constructor() {
    console.log('WebSocketService initialized - ready for real-time stock updates');
  }
  
  /**
   * Connect to FinStream Broadcasting Service WebSocket
   * 
   * Uses JWT token from AuthService for authentication.
   * Establishes STOMP connection with automatic reconnection on failure.
   */
  public connect(): void {
    if (this.stompClient?.connected) {
      console.log('WebSocket already connected');
      return;
    }
    
    const token = this.authService.getAccessToken();
    if (!token) {
      this._error.set('No JWT token available - please login first');
      console.error('WebSocket connection failed: No JWT token');
      return;
    }
    
    this._connectionStatus.set('connecting');
    this._error.set(null);
    
    try {
      // Create SockJS socket for corporate firewall compatibility
      const socket = new SockJS(this.WS_URL);
      
      // Configure STOMP client
      this.stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          'Authorization': `Bearer ${token}`,
          'X-Requested-With': 'XMLHttpRequest'
        },
        heartbeatIncoming: 25000, // 25 seconds - match server config
        heartbeatOutgoing: 25000,
        reconnectDelay: this.RECONNECT_DELAY,
        debug: (msg: string) => {
          console.debug('STOMP Debug:', msg);
        }
      });
      
      // Connection success handler
      this.stompClient.onConnect = (frame: Frame) => {
        this.onWebSocketConnected(frame);
      };
      
      // Connection error handler  
      this.stompClient.onStompError = (frame: Frame) => {
        this.onWebSocketError(frame);
      };
      
      // WebSocket error handler
      this.stompClient.onWebSocketError = (error: Event) => {
        this.onWebSocketError(null, error);
      };
      
      // Disconnection handler
      this.stompClient.onDisconnect = () => {
        this.onWebSocketDisconnected();
      };
      
      // Activate connection
      this.stompClient.activate();
      
      console.log('WebSocket connection initiated to:', this.WS_URL);
      
    } catch (error) {
      this.handleConnectionError('Failed to initialize WebSocket connection', error);
    }
  }
  
  /**
   * Disconnect from WebSocket service
   * 
   * Performs clean disconnection and resets state.
   */
  public disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    
    this._connectionStatus.set('disconnected');
    this._reconnectAttempts.set(0);
    console.log('WebSocket disconnected');
  }
  
  /**
   * Handle successful WebSocket connection
   * 
   * Subscribes to stock price updates and system notifications.
   */
  private onWebSocketConnected(frame: Frame): void {
    this._connectionStatus.set('connected');
    this._reconnectAttempts.set(0);
    this._error.set(null);
    
    console.log('WebSocket connected successfully:', frame);
    
    // Subscribe to real-time stock price updates
    this.stompClient?.subscribe('/topic/stocks/prices', (message: Message) => {
      this.handleStockPriceUpdate(message);
    });
    
    // Subscribe to system notifications
    this.stompClient?.subscribe('/topic/system/notifications', (message: Message) => {
      this.handleSystemNotification(message);
    });
    
    console.log('WebSocket subscriptions active: stock prices, system notifications');
  }
  
  /**
   * Handle WebSocket connection errors
   */
  private onWebSocketError(frame: Frame | null, error?: Event): void {
    const errorMsg = frame?.headers?.['message'] || 'WebSocket connection error';
    this.handleConnectionError(errorMsg, error);
  }
  
  /**
   * Handle WebSocket disconnection
   * 
   * Attempts automatic reconnection for banking reliability.
   */
  private onWebSocketDisconnected(): void {
    this._connectionStatus.set('disconnected');
    
    const attempts = this._reconnectAttempts();
    if (attempts < this.MAX_RECONNECT_ATTEMPTS) {
      this._reconnectAttempts.set(attempts + 1);
      
      console.log(`WebSocket disconnected - attempting reconnection ${attempts + 1}/${this.MAX_RECONNECT_ATTEMPTS}`);
      
      // Automatic reconnection with exponential backoff
      const delay = this.RECONNECT_DELAY * Math.pow(1.5, attempts);
      setTimeout(() => {
        if (this.connectionStatus() === 'disconnected') {
          this.connect();
        }
      }, delay);
    } else {
      this._error.set('Max reconnection attempts exceeded - please refresh page');
      console.error('WebSocket reconnection failed - max attempts exceeded');
    }
  }
  
  /**
   * Handle incoming stock price updates from WebSocket
   * 
   * Delegates to StockStateService for centralized state management.
   * StockStateService handles data validation, enhancement, and formatting.
   */
  private handleStockPriceUpdate(message: Message): void {
    try {
      const stockPrice: StockPrice = JSON.parse(message.body);
      
      // Validate basic message structure
      if (!stockPrice || !stockPrice.symbol) {
        console.warn('Invalid stock price message received:', message.body);
        return;
      }
      
      // Delegate to centralized state service
      this.stockStateService.updateStockPrice(stockPrice);
      
      console.debug(`WebSocket stock price received: ${stockPrice.symbol} = ${stockPrice.price}`);
      
    } catch (error) {
      console.error('Failed to process WebSocket stock price update:', error);
      this.stockStateService.addError({
        code: 'WEBSOCKET_MESSAGE_PARSE_ERROR',
        message: 'Failed to parse WebSocket stock price message',
        originalError: error,
        timestamp: new Date(),
        context: { messageBody: message.body }
      });
    }
  }
  
  /**
   * Handle system notifications from WebSocket
   * 
   * Delegates to StockStateService for centralized notification management.
   */
  private handleSystemNotification(message: Message): void {
    try {
      const notification: SystemNotification = JSON.parse(message.body);
      
      // Validate notification structure
      if (!notification || !notification.type || !notification.message) {
        console.warn('Invalid system notification received:', message.body);
        return;
      }
      
      // Delegate to centralized state service
      this.stockStateService.addSystemNotification(notification);
      
      console.info('WebSocket system notification received:', notification.message);
      
    } catch (error) {
      console.error('Failed to process WebSocket system notification:', error);
    }
  }
  
  /**
   * Handle connection errors with detailed logging
   */
  private handleConnectionError(message: string, error?: any): void {
    this._connectionStatus.set('error');
    this._error.set(message);
    
    console.error('WebSocket Connection Error:', message, error);
    
    // Add error to centralized error tracking
    this.stockStateService.addError({
      code: this.determineErrorCode(message, error),
      message: message,
      originalError: error,
      timestamp: new Date(),
      context: { 
        reconnectAttempts: this.reconnectAttempts(),
        wsUrl: this.WS_URL
      }
    });
    
    // Check if error is authentication-related
    if (message.includes('401') || message.includes('authentication') || message.includes('JWT')) {
      console.error('WebSocket authentication failed - token may be expired');
      // Future: Trigger token refresh via AuthService
    }
  }
  
  /**
   * Determine appropriate error code for centralized error tracking
   */
  private determineErrorCode(message: string, error?: any): any {
    if (message.includes('401') || message.includes('authentication') || message.includes('JWT')) {
      return 'WEBSOCKET_AUTHENTICATION_FAILED';
    }
    if (message.includes('connection') || message.includes('network')) {
      return 'WEBSOCKET_CONNECTION_FAILED';
    }
    if (error && error.type === 'NetworkError') {
      return 'NETWORK_ERROR';
    }
    return 'UNKNOWN_ERROR';
  }
  
  /**
   * Manual reconnection trigger
   * 
   * Allows components to trigger reconnection attempts.
   */
  public reconnect(): void {
    if (!this.canReconnect()) {
      console.warn('Cannot reconnect: already connected or max attempts exceeded');
      return;
    }
    
    console.log('Manual reconnection triggered');
    this.disconnect();
    setTimeout(() => this.connect(), 1000);
  }
  
  /**
   * Reset reconnection attempts
   * 
   * Useful after successful connection or manual reset.
   */
  public resetReconnectionAttempts(): void {
    this._reconnectAttempts.set(0);
    this._error.set(null);
    console.log('Reconnection attempts reset');
  }
}
