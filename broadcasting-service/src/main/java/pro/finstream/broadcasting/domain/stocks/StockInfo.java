package pro.finstream.broadcasting.domain.stocks;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.error.BaseException;
import pro.finstream.broadcasting.common.error.ErrorCode;
import pro.finstream.broadcasting.domain.Endpoints;
import pro.finstream.broadcasting.domain.prices.StockPrice;
import pro.finstream.broadcasting.domain.prices.StockPriceCache;
import pro.finstream.broadcasting.domain.stocks.model.StockEntity;
import pro.finstream.broadcasting.domain.stocks.model.StockRepository;

@RestController
@RequiredArgsConstructor
public class StockInfo {

    public record StockDetailResponse(
        String symbol,
        String companyName,
        String sector,
        Long marketCap,
        BigDecimal currentPrice,
        BigDecimal change,
        BigDecimal changePercent,
        LocalDateTime priceTimestamp,
        LocalDateTime lastUpdated
    ) {}

    private final StockRepository stockRepository;
    private final StockPriceCache stockPriceService;

    @GetMapping(Endpoints.API_STOCK)
    public StockDetailResponse getStockDetail(@RequestParam @NotEmpty @Pattern(regexp = "^[A-Z0-9.-]{1,15}$") String symbol) {
        String normalizedSymbol = symbol.trim().toUpperCase();

        StockEntity stock = stockRepository.findById(normalizedSymbol)
            .orElseThrow(() -> new BaseException(ErrorCode.E_NOT_FOUND, HttpStatus.NOT_FOUND, "symbol", symbol));
        
        StockPrice price = stockPriceService.getCurrentPrice(normalizedSymbol);
        
        BigDecimal currentPrice = null;
        BigDecimal change = null;
        BigDecimal changePercent = null;
        LocalDateTime priceTimestamp = null;
        
        if (price != null) {
            currentPrice = price.price();
            change = price.change();
            priceTimestamp = price.timestamp() != null 
                ? price.timestamp().atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
                : null;

            if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0 && change != null) {
                changePercent = change.divide(currentPrice.subtract(change), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
        }
        
        return new StockDetailResponse(
            stock.getSymbol(),
            stock.getCompany(),
            stock.getSector(),
            stock.getMarketCap(),
            currentPrice,
            change,
            changePercent,
            priceTimestamp,
            stock.getUpdated() != null ? stock.getUpdated().atZone(java.time.ZoneOffset.UTC).toLocalDateTime() : null
        );
    }
}
