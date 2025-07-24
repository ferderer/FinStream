package pro.finstream.broadcasting.domain.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.domain.Endpoints;
import pro.finstream.broadcasting.domain.prices.StockPrice;
import pro.finstream.broadcasting.domain.prices.StockPriceCache;
import pro.finstream.broadcasting.domain.stocks.model.StockRepository;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistRepository;

@RestController
@RequiredArgsConstructor
public class LoadPrices implements CurrentUserAccessor {

    public record WatchlistPriceResponse(
        String symbol,
        String companyName,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePercent,
        LocalDateTime lastUpdated
    ) {}

    public record WatchlistPricesResponse(
        List<WatchlistPriceResponse> prices,
        int totalSymbols,
        int pricesAvailable,
        LocalDateTime timestamp
    ) {}

    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;
    private final StockPriceCache stockPriceService;

    @GetMapping(Endpoints.API_PRICES)
    public WatchlistPricesResponse getWatchlistPrices() {
        Long userId = currentUserId();
        
        // Get user's watchlist symbols
        Set<String> symbols = watchlistRepository.findSymbolsByUserId(userId);
        
        if (symbols.isEmpty()) {
            return new WatchlistPricesResponse(List.of(), 0, 0, LocalDateTime.now());
        }
        
        // Get current prices
        List<StockPrice> prices = stockPriceService.getCurrentPrices(symbols);
        Map<String, StockPrice> priceMap = prices.stream()
            .collect(Collectors.toMap(StockPrice::symbol, price -> price));
        
        // Get company names
        Map<String, String> companyNames = stockRepository.findBySymbolIn(symbols)
            .stream()
            .collect(Collectors.toMap(
                stock -> stock.getSymbol(),
                stock -> stock.getCompany()
            ));
        
        // Build response for all symbols (including those without prices)
        List<WatchlistPriceResponse> priceResponses = symbols.stream()
            .sorted()
            .map(symbol -> {
                StockPrice price = priceMap.get(symbol);
                String companyName = companyNames.getOrDefault(symbol, symbol);
                
                if (price != null) {
                    BigDecimal changePercent = price.price() != null && price.price().compareTo(BigDecimal.ZERO) > 0 && price.change() != null
                        ? price.change().divide(price.price().subtract(price.change()), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                    
                    return new WatchlistPriceResponse(
                        symbol,
                        companyName,
                        price.price(),
                        price.change(),
                        changePercent,
                        price.timestamp() != null ? price.timestamp().atZone(java.time.ZoneOffset.UTC).toLocalDateTime() : null
                    );
                }
                else {
                    return new WatchlistPriceResponse(symbol, companyName, null, null, null, null);
                }
            })
            .toList();
        
        return new WatchlistPricesResponse(priceResponses, symbols.size(), prices.size(), LocalDateTime.now());
    }
}
