package pro.finstream.stock.messaging;

import java.math.BigDecimal;
import java.time.Instant;

public record StockPriceEvent(
    String symbol,
    BigDecimal price,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal high,
    BigDecimal low,
    Long timestamp,
    String source,
    Instant processedAt
) {}
