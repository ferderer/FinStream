package pro.finstream.broadcasting.domain.watchlist;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import static pro.finstream.broadcasting.domain.Endpoints.*;
import pro.finstream.broadcasting.domain.prices.StockPrice;
import pro.finstream.broadcasting.domain.prices.StockPriceCache;
import pro.finstream.broadcasting.domain.stocks.model.StockEntity;
import pro.finstream.broadcasting.domain.stocks.model.StockRepository;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistItem;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistItemEntity;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistRepository;

@RestController
@RequiredArgsConstructor
public class LoadItems implements CurrentUserAccessor {

    public record WatchlistResponse(
        List<WatchlistItem> items,
        BigDecimal totalValue,
        LocalDateTime timestamp
    ) {}
    
    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;
    private final StockPriceCache stockPriceService;

    @GetMapping(API_WATCHLIST)
    public WatchlistResponse load() {
        List<WatchlistItemEntity> watchlistItems = watchlistRepository.findByUserIdOrderByAddedAtDesc(currentUserId());
        
        if (watchlistItems.isEmpty()) {
            return new WatchlistResponse(List.of(), BigDecimal.ZERO, LocalDateTime.now());
        }

        Set<String> symbols = watchlistItems.stream()
            .map(WatchlistItemEntity::getSymbol)
            .collect(Collectors.toSet());

        Map<String, StockPrice> priceMap = stockPriceService.getCurrentPrices(symbols)
            .stream()
            .collect(Collectors.toMap(StockPrice::symbol, price -> price));

        Map<String, String> companyNames = stockRepository.findBySymbolIn(symbols)
            .stream()
            .collect(Collectors.toMap(
                (StockEntity stock) -> stock.getSymbol(),
                (StockEntity stock) -> stock.getCompany()
            ));

        List<WatchlistItem> responseItems = watchlistItems.stream()
            .map(item -> {
                StockPrice price = priceMap.get(item.getSymbol());
                String companyName = companyNames.getOrDefault(item.getSymbol(), item.getSymbol());
                
                if (price != null) {
                    BigDecimal changePercent = price.price() != null && price.price().compareTo(BigDecimal.ZERO) > 0 && price.change() != null
                        ? price.change().divide(price.price().subtract(price.change()), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                    
                    return new WatchlistItem(
                        item.getId(),
                        item.getSymbol(),
                        companyName,
                        item.getNotes(),
                        item.getAdded(),
                        price.price(),
                        price.change(),
                        changePercent
                    );
                }
                else {
                    return new WatchlistItem(
                        item.getId(),
                        item.getSymbol(),
                        companyName,
                        item.getNotes(),
                        item.getAdded(),
                        null,
                        null,
                        null
                    );
                }
            })
            .toList();

        BigDecimal totalValue = responseItems.stream()
            .filter(item -> item.currentPrice() != null)
            .map(WatchlistItem::currentPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new WatchlistResponse(responseItems, totalValue, LocalDateTime.now());
    }
}
