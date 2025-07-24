package pro.finstream.broadcasting.domain.prices;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record StockPrice(
    String symbol,
    BigDecimal price,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal high,
    BigDecimal low,
    Instant timestamp,
    String source,
    LocalDateTime processedAt
) {
    public boolean isPositiveChange() {
        return change != null && change.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegativeChange() {
        return change != null && change.compareTo(BigDecimal.ZERO) < 0;
    }

    public String getFormattedChange() {
        if (change == null) return "0.00";
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change.toString();
    }

    public String getFormattedChangePercent() {
        if (changePercent == null) return "0.00%";
        return (changePercent.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + changePercent + "%";
    }
}
