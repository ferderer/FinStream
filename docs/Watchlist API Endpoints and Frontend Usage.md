# API Endpoints and Frontend Usage

## Watchlist Management

### `GET /api/watchlist`
**Purpose**: Load user's complete watchlist with current prices
**Frontend Usage**: 
- **Dashboard page load** - populate watchlist grid
- **Periodic refresh** - update prices every 30 seconds
- **After WebSocket reconnect** - sync state

**Response**:
```json
{
  "items": [
    {
      "id": "uuid",
      "symbol": "AAPL",
      "companyName": "Apple Inc.",
      "notes": "Long term hold",
      "addedAt": "2024-01-15T10:30:00Z",
      "currentPrice": 150.25,
      "change": 2.15,
      "changePercent": 1.45
    }
  ],
  "totalValue": 45250.75,
  "count": 12
}
```

### `POST /api/watchlist`
**Purpose**: Add stock to user's watchlist
**Frontend Usage**:
- **Stock search modal** - "Add to Watchlist" button
- **Bulk import** - paste symbol list
- **Quick add** - top navigation search bar

**Request**: `{ "symbol": "AAPL", "notes": "Optional note" }`

### `DELETE /api/watchlist/{symbol}`
**Purpose**: Remove stock from watchlist
**Frontend Usage**:
- **Watchlist grid** - trash can icon per row
- **Bulk actions** - select multiple and delete
- **Context menu** - right-click options

### `PUT /api/watchlist/{symbol}/notes`
**Purpose**: Update notes for watchlist item
**Frontend Usage**:
- **Inline editing** - click notes cell to edit
- **Detail modal** - expanded view with notes field

**Request**: `{ "notes": "Updated investment thesis" }`

## Price Alerts

### `GET /api/alerts`
**Purpose**: Load user's price alerts
**Frontend Usage**:
- **Alerts tab** - dedicated alerts management page
- **Dashboard widget** - show active alert count
- **Stock detail modal** - show alerts for specific stock

### `POST /api/alerts`
**Purpose**: Create new price alert
**Frontend Usage**:
- **Stock detail modal** - "Create Alert" button
- **Bulk alert creation** - set alerts for multiple stocks
- **Chart interaction** - click price level to set alert

**Request**:
```json
{
  "symbol": "AAPL",
  "alertType": "ABOVE",
  "targetPrice": 160.00
}
```

### `PUT /api/alerts/{id}`
**Purpose**: Update existing alert
**Frontend Usage**:
- **Alerts management** - edit alert parameters
- **Quick disable/enable** - toggle active state

### `DELETE /api/alerts/{id}`
**Purpose**: Delete price alert
**Frontend Usage**:
- **Alerts table** - delete button per row
- **Alert notification** - "Dismiss" action after trigger

## Dashboard & Analytics

### `GET /api/dashboard/summary`
**Purpose**: Portfolio overview data
**Frontend Usage**:
- **Dashboard header** - KPI cards (total value, day change, etc.)
- **Portfolio widget** - pie chart of holdings
- **Performance summary** - top/bottom performers

**Response**:
```json
{
  "watchlistCount": 12,
  "totalValue": 45250.75,
  "totalChange": 1250.30,
  "totalChangePercent": 2.84,
  "topMovers": [
    { "symbol": "NVDA", "changePercent": 8.5 },
    { "symbol": "TSLA", "changePercent": -4.2 }
  ],
  "sectorBreakdown": {
    "Technology": 65.5,
    "Healthcare": 20.2,
    "Finance": 14.3
  }
}
```

### `GET /api/dashboard/watchlist-prices`
**Purpose**: Real-time prices for dashboard updates
**Frontend Usage**:
- **Fallback for WebSocket** - when connection drops
- **Initial load** - before WebSocket connects
- **Background sync** - verify WebSocket data accuracy

## Stock Search & Metadata

### `GET /api/stocks/search?q={query}`
**Purpose**: Search for stocks to add to watchlist
**Frontend Usage**:
- **Add stock modal** - autocomplete dropdown
- **Quick search** - navigation bar search
- **Symbol validation** - verify before adding

**Response**:
```json
{
  "results": [
    {
      "symbol": "AAPL",
      "companyName": "Apple Inc.",
      "sector": "Technology",
      "marketCap": 3000000000000
    }
  ]
}
```

### `GET /api/stocks/{symbol}`
**Purpose**: Get detailed stock information
**Frontend Usage**:
- **Stock detail modal** - company info, sector, market cap
- **Watchlist enrichment** - populate missing metadata

## User Settings

### `GET /api/settings`
**Purpose**: Load user preferences
**Frontend Usage**:
- **Settings page** - populate form fields
- **App initialization** - set timezone, currency, notifications

### `PUT /api/settings`
**Purpose**: Update user preferences
**Frontend Usage**:
- **Settings page** - save preferences form
- **Quick toggles** - notification on/off switch

## Frontend Integration Patterns

### **Real-time Updates Flow**
1. **Initial Load**: REST API calls populate data
2. **WebSocket Connect**: Subscribe to user's watchlist symbols
3. **Live Updates**: WebSocket pushes price changes
4. **Fallback**: REST API polling if WebSocket fails

### **State Management (Angular Signals)**
```typescript
// Watchlist state
watchlist = signal<WatchlistItem[]>([]);
watchlistLoading = signal(false);

// Load initial data
loadWatchlist() {
  this.watchlistLoading.set(true);
  this.http.get<WatchlistResponse>('/api/watchlist')
    .subscribe(data => {
      this.watchlist.set(data.items);
      this.watchlistLoading.set(false);
    });
}

// WebSocket updates
onPriceUpdate(update: PriceUpdate) {
  this.watchlist.update(items => 
    items.map(item => 
      item.symbol === update.symbol 
        ? { ...item, currentPrice: update.price, change: update.change }
        : item
    )
  );
}
```

### **Error Handling**
- **Network errors**: Show toast notification, retry logic
- **Validation errors**: Inline form validation
- **Server errors**: Global error handler with user-friendly messages

### **Optimistic Updates**
- **Add to watchlist**: Immediately add to UI, rollback on error
- **Delete item**: Remove from UI, restore on error
- **Update notes**: Show changes immediately, sync in background

### **Performance Optimizations**
- **Virtual scrolling**: For large watchlists (100+ items)
- **Debounced search**: 300ms delay for stock search
- **Lazy loading**: Load chart data only when needed
- **WebSocket filtering**: Only receive updates for watchlisted stocks
