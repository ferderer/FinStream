package pro.finstream.broadcasting.domain.prices;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class StockPriceCache {
    
    private final Cache<String, StockPrice> priceCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build();
    
    public List<StockPrice> getCurrentPrices(Set<String> symbols) {
        return symbols.stream()
            .map(priceCache::getIfPresent)
            .filter(Objects::nonNull)
            .toList();
    }

    public void updatePrice(StockPrice stockPrice) {
        priceCache.put(stockPrice.symbol(), stockPrice);
    }

    public StockPrice getCurrentPrice(String symbol) {
        return priceCache.getIfPresent(symbol);
    }
}
