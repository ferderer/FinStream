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
import pro.finstream.broadcasting.domain.alerts.model.PriceAlertRepository;
import pro.finstream.broadcasting.domain.prices.StockPrice;
import pro.finstream.broadcasting.domain.prices.StockPriceCache;
import pro.finstream.broadcasting.domain.stocks.model.StockRepository;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistRepository;

@RestController
@RequiredArgsConstructor
public class LoadSummary implements CurrentUserAccessor {
    public record TopMoverResponse(
        String symbol,
        String companyName,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePercent
    ) {}

    public record SectorBreakdownResponse(
        String sector,
        int count,
        BigDecimal percentage
    ) {}

    public record DashboardSummaryResponse(
        int watchlistCount,
        int activeAlertsCount,
        BigDecimal totalValue,
        BigDecimal totalChange,
        BigDecimal totalChangePercent,
        List<TopMoverResponse> topGainers,
        List<TopMoverResponse> topLosers,
        List<SectorBreakdownResponse> sectorBreakdown,
        LocalDateTime timestamp
    ) {}

    private final WatchlistRepository watchlistRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final StockRepository stockRepository;
    private final StockPriceCache stockPriceService;

    @GetMapping(Endpoints.API_DASHBOARD)
    public DashboardSummaryResponse getDashboardSummary() {
        Long userId = currentUserId();
        
        // Get basic counts
        long watchlistCount = watchlistRepository.countByUserId(userId);
        long activeAlertsCount = priceAlertRepository.countByUserIdAndActive(userId, true);
        
        // Get user's watchlist symbols
        Set<String> symbols = watchlistRepository.findSymbolsByUserId(userId);
        
        if (symbols.isEmpty()) {
            return new DashboardSummaryResponse(
                (int) watchlistCount,
                (int) activeAlertsCount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now()
            );
        }
        
        // Get current prices
        List<StockPrice> prices = stockPriceService.getCurrentPrices(symbols);
        Map<String, StockPrice> priceMap = prices.stream()
            .collect(Collectors.toMap(StockPrice::symbol, price -> price));
        
        // Get stock metadata for company names and sectors
        Map<String, String> companyNames = stockRepository.findBySymbolIn(symbols)
            .stream()
            .collect(Collectors.toMap(
                stock -> stock.getSymbol(),
                stock -> stock.getCompany()
            ));
        
        Map<String, String> sectors = stockRepository.findBySymbolIn(symbols)
            .stream()
            .collect(Collectors.toMap(
                stock -> stock.getSymbol(),
                stock -> stock.getSector() != null ? stock.getSector() : "Unknown"
            ));
        
        // Calculate totals (assuming 1 share per stock)
        BigDecimal totalValue = prices.stream()
            .filter(price -> price.price() != null)
            .map(StockPrice::price)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalChange = prices.stream()
            .filter(price -> price.change() != null)
            .map(StockPrice::change)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalChangePercent = totalValue.compareTo(BigDecimal.ZERO) > 0
            ? totalChange.divide(totalValue.subtract(totalChange), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        // Find top movers
        List<TopMoverResponse> topGainers = prices.stream()
            .filter(price -> price.change() != null && price.change().compareTo(BigDecimal.ZERO) > 0)
            .sorted((p1, p2) -> p2.change().compareTo(p1.change()))
            .limit(3)
            .map(price -> createTopMoverResponse(price, companyNames))
            .toList();
        
        List<TopMoverResponse> topLosers = prices.stream()
            .filter(price -> price.change() != null && price.change().compareTo(BigDecimal.ZERO) < 0)
            .sorted((p1, p2) -> p1.change().compareTo(p2.change()))
            .limit(3)
            .map(price -> createTopMoverResponse(price, companyNames))
            .toList();
        
        // Calculate sector breakdown
        Map<String, Long> sectorCounts = symbols.stream()
            .collect(Collectors.groupingBy(
                symbol -> sectors.getOrDefault(symbol, "Unknown"),
                Collectors.counting()
            ));
        
        List<SectorBreakdownResponse> sectorBreakdown = sectorCounts.entrySet().stream()
            .map(entry -> new SectorBreakdownResponse(
                entry.getKey(),
                entry.getValue().intValue(),
                BigDecimal.valueOf(entry.getValue() * 100.0 / symbols.size())
                    .setScale(1, RoundingMode.HALF_UP)
            ))
            .sorted((s1, s2) -> Integer.compare(s2.count(), s1.count()))
            .toList();
        
        return new DashboardSummaryResponse(
            (int) watchlistCount,
            (int) activeAlertsCount,
            totalValue,
            totalChange,
            totalChangePercent,
            topGainers,
            topLosers,
            sectorBreakdown,
            LocalDateTime.now()
        );
    }
    
    private TopMoverResponse createTopMoverResponse(StockPrice price, Map<String, String> companyNames) {
        BigDecimal changePercent = price.price() != null && 
            price.price().compareTo(BigDecimal.ZERO) > 0 &&
            price.change() != null
            ? price.change().divide(
                price.price().subtract(price.change()), 
                4, RoundingMode.HALF_UP
              ).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        return new TopMoverResponse(
            price.symbol(),
            companyNames.getOrDefault(price.symbol(), price.symbol()),
            price.price(),
            price.change(),
            changePercent
        );
    }
}
