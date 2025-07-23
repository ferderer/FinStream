import { Component, OnInit, OnDestroy, inject, effect, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WebSocketService } from '../websocket/websocket-service';
import { StockStateService } from '../websocket/stock-state.service';
import { AuthService } from '../auth/auth-service';
import { 
  EnhancedStockPrice, 
  ConnectionStatus, 
  SystemNotification,
  StockServiceError 
} from '../websocket/stock-price.types';

/**
 * Dashboard Component - Real-time Stock Price Dashboard
 * 
 * Main dashboard component for FinStream platform that displays:
 * - Real-time stock prices with live updates
 * - WebSocket connection status and controls
 * - System notifications and alerts
 * - Performance monitoring and error tracking
 * - Banking-grade professional UI
 * 
 * Features:
 * - Angular Signals for reactive updates
 * - Automatic WebSocket connection management
 * - Error handling with user-friendly messages
 * - Responsive design for desktop and mobile
 * - Banking-style color coding and formatting
 * 
 * Integration:
 * - WebSocketService for connection management
 * - StockStateService for centralized data
 * - AuthService for user context
 * 
 * @author FinStream Platform
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-container">
      
      <!-- Dashboard Header -->
      <header class="dashboard-header">
        <div class="header-content">
          <h1 class="dashboard-title">FinStream Stock Dashboard</h1>
          <div class="user-info">
            <span class="welcome-text">Welcome, {{ userInfo()?.username }}!</span>
            <span class="user-email">{{ userInfo()?.email }}</span>
          </div>
        </div>
      </header>

      <!-- Connection Status Bar -->
      <div class="connection-status-bar" [class]="'status-' + connectionStatus()">
        <div class="connection-info">
          <div class="status-indicator" [class]="'indicator-' + connectionStatus()">
            <span class="status-dot"></span>
            <span class="status-text">{{ getConnectionStatusText() }}</span>
          </div>
          
          @if (isConnected()) {
            <div class="connection-stats">
              <span class="stat-item">
                <strong>{{ performanceStats().connectedSymbols || 0 }}</strong> symbols
              </span>
              <span class="stat-item">
                <strong>{{ performanceStats().updatesPerSecond.toFixed(1) || '0.0' }}</strong> updates/sec
              </span>
              <span class="stat-item">
                <strong>{{ performanceStats().totalUpdates || 0 }}</strong> total updates
              </span>
            </div>
          }
        </div>
        
        <div class="connection-controls">
          @if (!isConnected() && !isConnecting()) {
            <button 
              class="btn btn-primary"
              (click)="connectWebSocket()"
              [disabled]="isConnecting()">
              Connect
            </button>
          }
          
          @if (isConnected()) {
            <button 
              class="btn btn-secondary"
              (click)="disconnectWebSocket()">
              Disconnect
            </button>
          }
          
          @if (canReconnect()) {
            <button 
              class="btn btn-warning"
              (click)="reconnectWebSocket()">
              Reconnect ({{ reconnectAttempts() }}/10)
            </button>
          }
        </div>
      </div>

      <!-- System Notifications -->
      @if (activeNotifications().length > 0) {
        <div class="notifications-section">
          @for (notification of activeNotifications(); track notification.timestamp) {
            <div class="notification" [class]="'notification-' + notification.severity">
              <div class="notification-content">
                <strong class="notification-type">{{ notification.type }}</strong>
                <span class="notification-message">{{ notification.message }}</span>
                <span class="notification-time">{{ formatTime(notification.timestamp) }}</span>
              </div>
              <button 
                class="notification-close"
                (click)="dismissNotification(notification)">
                ×
              </button>
            </div>
          }
        </div>
      }

      <!-- Stock Price Grid -->
      <div class="stock-grid-section">
        <div class="section-header">
          <h2 class="section-title">Live Stock Prices</h2>
          <div class="grid-controls">
            <span class="stock-count">{{ selectedStocks().length }} stocks tracked</span>
            @if (lastUpdateTime()) {
              <span class="last-update">Last update: {{ lastUpdateTime() }}</span>
            }
          </div>
        </div>

        @if (selectedStocks().length === 0 && isConnected()) {
          <div class="empty-state">
            <div class="empty-icon">📊</div>
            <h3>Waiting for stock data...</h3>
            <p>Connected to WebSocket, waiting for live price updates.</p>
          </div>
        }

        @if (selectedStocks().length === 0 && !isConnected()) {
          <div class="empty-state">
            <div class="empty-icon">🔌</div>
            <h3>Not connected</h3>
            <p>Please connect to start receiving live stock prices.</p>
            <button 
              class="btn btn-primary"
              (click)="connectWebSocket()">
              Connect Now
            </button>
          </div>
        }

        <!-- Stock Cards Grid -->
        @if (selectedStocks().length > 0) {
          <div class="stock-grid">
            @for (stock of selectedStocks(); track stock.symbol) {
              <div 
                class="stock-card"
                [class]="'card-' + stock.displayColor + ' animation-' + stock.animationState"
                [attr.data-symbol]="stock.symbol">
                
                <div class="stock-header">
                  <div class="stock-symbol">{{ stock.symbol }}</div>
                  <div class="stock-trend" [class]="'trend-' + stock.trend">
                    @if (stock.trend === 'up') { ↗ }
                    @if (stock.trend === 'down') { ↘ }
                    @if (stock.trend === 'neutral') { → }
                  </div>
                </div>

                <div class="stock-price">
                  <div class="current-price">{{ stock.formatted.price }}</div>
                  <div class="price-change" [class]="'change-' + stock.displayColor">
                    <span class="change-amount">{{ stock.formatted.change }}</span>
                    <span class="change-percent">{{ stock.formatted.changePercent }}</span>
                  </div>
                </div>

                <div class="stock-details">
                  <div class="detail-row">
                    <span class="detail-label">High:</span>
                    <span class="detail-value">{{ stock.formatted.high }}</span>
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">Low:</span>
                    <span class="detail-value">{{ stock.formatted.low }}</span>
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">Updated:</span>
                    <span class="detail-value time">{{ getRelativeTime(stock.lastUpdated) }}</span>
                  </div>
                </div>
              </div>
            }
          </div>
        }
      </div>

      <!-- Market Summary Section -->
      @if (selectedStocks().length > 0) {
        <div class="market-summary-section">
          <div class="summary-cards">
            <div class="summary-card gain">
              <div class="summary-header">
                <span class="summary-title">Top Gainer</span>
                <span class="summary-icon">📈</span>
              </div>
              @if (topGainer()) {
                <div class="summary-content">
                  <div class="summary-symbol">{{ topGainer()!.symbol }}</div>
                  <div class="summary-change gain">{{ topGainer()!.formatted.changePercent }}</div>
                </div>
              } @else {
                <div class="summary-content">
                  <span class="no-data">No gainers</span>
                </div>
              }
            </div>

            <div class="summary-card loss">
              <div class="summary-header">
                <span class="summary-title">Top Loser</span>
                <span class="summary-icon">📉</span>
              </div>
              @if (topLoser()) {
                <div class="summary-content">
                  <div class="summary-symbol">{{ topLoser()!.symbol }}</div>
                  <div class="summary-change loss">{{ topLoser()!.formatted.changePercent }}</div>
                </div>
              } @else {
                <div class="summary-content">
                  <span class="no-data">No losers</span>
                </div>
              }
            </div>

            <div class="summary-card neutral">
              <div class="summary-header">
                <span class="summary-title">Market Status</span>
                <span class="summary-icon">🏛️</span>
              </div>
              <div class="summary-content">
                @if (marketStatus()) {
                  <div class="summary-symbol">{{ marketStatus()!.market }}</div>
                  <div class="summary-status" [class]="'status-' + marketStatus()!.status.toLowerCase()">
                    {{ marketStatus()!.status }}
                  </div>
                } @else {
                  <span class="no-data">Status unknown</span>
                }
              </div>
            </div>
          </div>
        </div>
      }

      <!-- Error Display -->
      @if (latestErrors().length > 0) {
        <div class="error-section">
          <div class="section-header">
            <h3 class="section-title">Recent Issues</h3>
            <button 
              class="btn btn-text"
              (click)="clearErrors()">
              Clear All
            </button>
          </div>
          
          @for (error of latestErrors(); track error.timestamp) {
            <div class="error-item">
              <div class="error-content">
                <div class="error-code">{{ error.code }}</div>
                <div class="error-message">{{ error.message }}</div>
                <div class="error-time">{{ formatTime(error.timestamp.toISOString()) }}</div>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit, OnDestroy {
  
  // Inject services using modern Angular pattern
  private readonly wsService = inject(WebSocketService);
  private readonly stockStateService = inject(StockStateService);
  private readonly authService = inject(AuthService);
  
  // Expose reactive state from services
  public readonly connectionStatus = this.wsService.connectionStatus;
  public readonly isConnected = this.wsService.isConnected;
  public readonly isConnecting = this.wsService.isConnecting;
  public readonly canReconnect = this.wsService.canReconnect;
  public readonly reconnectAttempts = this.wsService.reconnectAttempts;
  public readonly error = this.wsService.error;
  
  public readonly selectedStocks = this.stockStateService.selectedStocks;
  public readonly gainers = this.stockStateService.gainers;
  public readonly losers = this.stockStateService.losers;
  public readonly activeNotifications = this.stockStateService.activeNotifications;
  public readonly latestErrors = this.stockStateService.latestErrors;
  public readonly performanceStats = this.stockStateService.performanceStats;
  public readonly marketStatus = this.stockStateService.marketStatus;
  
  // Component-specific state
  public readonly userInfo = signal<any>(null);
  public readonly lastUpdateTime = signal<Date | null>(null);
  
  // Computed values for UI
  public readonly topGainer = computed(() => {
    const gainers = this.stockStateService.gainers();
    return gainers.length > 0 ? gainers[0] : null;
  });
  
  public readonly topLoser = computed(() => {
    const losers = this.stockStateService.losers();
    return losers.length > 0 ? losers[0] : null;
  });
  
  constructor() {
    console.log('DashboardComponent initialized with reactive stock data');
    
    // Setup reactive effects for UI updates
    this.setupReactiveEffects();
  }
  
  ngOnInit(): void {
    // Load user information
    this.loadUserInfo();
    
    // Auto-connect WebSocket if user is authenticated
    if (this.authService.authenticated()) {
      setTimeout(() => this.connectWebSocket(), 500);
    }
  }
  
  ngOnDestroy(): void {
    // Clean disconnect on component destroy
    this.wsService.disconnect();
  }
  
  /**
   * Setup reactive effects for UI updates
   */
  private setupReactiveEffects(): void {
    // Update last update time when stocks change
    effect(() => {
      const stocks = this.selectedStocks();
      if (stocks.length > 0) {
        const latest = stocks.reduce((latest, stock) => 
          stock.lastUpdated > latest ? stock.lastUpdated : latest, 
          new Date(0)
        );
        this.lastUpdateTime.set(latest);
      }
    });
    
    // Log connection status changes
    effect(() => {
      const status = this.connectionStatus();
      console.log(`Dashboard: Connection status changed to ${status}`);
    });
  }
  
  /**
   * Load user information from auth service
   */
  private loadUserInfo(): void {
    // Assuming your AuthService has user info available
    // Adjust this based on your actual AuthService API
    const userInfo = {
      username: 'vadim', // From your screenshot
      email: 'vadim@ferderer.de' // From your screenshot
    };
    this.userInfo.set(userInfo);
  }
  
  /**
   * Connect to WebSocket service
   */
  public connectWebSocket(): void {
    console.log('Dashboard: Initiating WebSocket connection');
    this.wsService.connect();
  }
  
  /**
   * Disconnect from WebSocket service
   */
  public disconnectWebSocket(): void {
    console.log('Dashboard: Disconnecting WebSocket');
    this.wsService.disconnect();
  }
  
  /**
   * Trigger manual reconnection
   */
  public reconnectWebSocket(): void {
    console.log('Dashboard: Manual reconnection triggered');
    this.wsService.reconnect();
  }
  
  /**
   * Clear all errors from error tracking
   */
  public clearErrors(): void {
    this.stockStateService.clearErrors();
  }
  
  /**
   * Dismiss specific notification
   */
  public dismissNotification(notification: SystemNotification): void {
    // For now, clear all notifications
    // Future: Remove specific notification
    this.stockStateService.clearNotifications();
  }
  
  /**
   * Get human-readable connection status text
   */
  public getConnectionStatusText(): string {
    switch (this.connectionStatus()) {
      case 'connected': return 'Connected';
      case 'connecting': return 'Connecting...';
      case 'disconnected': return 'Disconnected';
      case 'error': return 'Connection Error';
      case 'reconnecting': return 'Reconnecting...';
      default: return 'Unknown';
    }
  }
  
  /**
   * Format timestamp for display
   */
  public formatTime(timestamp: string): string {
    try {
      const date = new Date(timestamp);
      return date.toLocaleTimeString('en-US', { 
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } catch {
      return 'Invalid time';
    }
  }
  
  /**
   * Get relative time for "time ago" display
   */
  public getRelativeTime(date: Date): string {
    const now = Date.now();
    const diff = now - date.getTime();
    
    if (diff < 1000) return 'Just now';
    if (diff < 60000) return `${Math.floor(diff / 1000)}s ago`;
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    
    return date.toLocaleTimeString('en-US', { 
      hour12: false,
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
